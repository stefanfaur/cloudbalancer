package com.cloudbalancer.agent.service;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.registration.AgentRegistrationClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContainerManagerTest {

    private DockerClient dockerClient;
    private AgentProperties props;
    private AgentRegistrationClient registrationClient;
    private ContainerManager containerManager;

    @BeforeEach
    void setUp() {
        dockerClient = mock(DockerClient.class);
        props = new AgentProperties();
        props.setId("agent-1");
        props.getDocker().setWorkerImage("docker-worker");
        props.getDocker().setNetworkName("docker_default");
        props.getDocker().setKafkaBootstrapInternal("kafka:29092");
        registrationClient = mock(AgentRegistrationClient.class);
        when(registrationClient.getCachedResult()).thenReturn(null); // local mode
        // Mock list containers (empty on startup)
        var listCmd = mock(ListContainersCmd.class);
        when(dockerClient.listContainersCmd()).thenReturn(listCmd);
        when(listCmd.withShowAll(true)).thenReturn(listCmd);
        when(listCmd.withNameFilter(any())).thenReturn(listCmd);
        when(listCmd.exec()).thenReturn(List.of());

        containerManager = new ContainerManager(dockerClient, props, registrationClient);
    }

    @Test
    void startWorkerCreatesAndStartsContainer() {
        var createCmd = mock(CreateContainerCmd.class, RETURNS_DEEP_STUBS);
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withEnv(any(List.class))).thenReturn(createCmd);
        when(createCmd.withHostConfig(any())).thenReturn(createCmd);
        var response = mock(CreateContainerResponse.class);
        when(response.getId()).thenReturn("container-123");
        when(createCmd.exec()).thenReturn(response);

        var startCmd = mock(StartContainerCmd.class);
        when(dockerClient.startContainerCmd("container-123")).thenReturn(startCmd);

        String containerId = containerManager.startWorker("worker-5", 4, 8192, "WORKER_ID=worker-5");
        assertThat(containerId).isEqualTo("container-123");
        verify(startCmd).exec();
    }

    @Test
    void stopWorkerStopsAndRemovesContainer() {
        // Setup: start a worker first
        var createCmd = mock(CreateContainerCmd.class, RETURNS_DEEP_STUBS);
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withEnv(any(List.class))).thenReturn(createCmd);
        when(createCmd.withHostConfig(any())).thenReturn(createCmd);
        var response = mock(CreateContainerResponse.class);
        when(response.getId()).thenReturn("container-123");
        when(createCmd.exec()).thenReturn(response);
        when(dockerClient.startContainerCmd("container-123")).thenReturn(mock(StartContainerCmd.class));

        containerManager.startWorker("worker-5", 4, 8192, "WORKER_ID=worker-5");

        // Now stop it
        var stopCmd = mock(StopContainerCmd.class);
        when(dockerClient.stopContainerCmd("container-123")).thenReturn(stopCmd);
        when(stopCmd.withTimeout(anyInt())).thenReturn(stopCmd);
        var removeCmd = mock(RemoveContainerCmd.class);
        when(dockerClient.removeContainerCmd("container-123")).thenReturn(removeCmd);
        when(removeCmd.withForce(true)).thenReturn(removeCmd);

        containerManager.stopWorker("worker-5");

        verify(stopCmd).exec();
        verify(removeCmd).exec();
    }

    @Test
    void stopWorkerSkipsWhenNotTracked() {
        containerManager.stopWorker("unknown-worker");
        verify(dockerClient, never()).stopContainerCmd(anyString());
    }

    @Test
    void getActiveWorkerIdsReturnsTrackedWorkers() {
        assertThat(containerManager.getActiveWorkerIds()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void startWorkerPassesInternalAddressesAsEnvVars() {
        var createCmd = mock(CreateContainerCmd.class, RETURNS_DEEP_STUBS);
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withEnv(any(List.class))).thenReturn(createCmd);
        when(createCmd.withHostConfig(any())).thenReturn(createCmd);
        var response = mock(CreateContainerResponse.class);
        when(response.getId()).thenReturn("container-456");
        when(createCmd.exec()).thenReturn(response);
        when(dockerClient.startContainerCmd("container-456")).thenReturn(mock(StartContainerCmd.class));

        containerManager.startWorker("worker-ext", 2, 4096, "WORKER_ID=worker-ext");

        var captor = ArgumentCaptor.forClass(List.class);
        verify(createCmd).withEnv(captor.capture());
        List<String> passedEnv = captor.getValue();

        assertThat(passedEnv)
            .contains("KAFKA_BOOTSTRAP_SERVERS=kafka:29092")
            .contains("DISPATCHER_URL=http://dispatcher:8080");
    }
}

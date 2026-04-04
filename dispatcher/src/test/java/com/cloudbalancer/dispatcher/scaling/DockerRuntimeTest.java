package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.config.ScalingProperties;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.HostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerRuntimeTest {

    @Mock DockerClient dockerClient;
    @Mock WorkerRegistryService workerRegistry;
    @Mock EventPublisher eventPublisher;
    @Mock CreateContainerCmd createContainerCmd;
    @Mock StartContainerCmd startContainerCmd;
    @Mock StopContainerCmd stopContainerCmd;
    @Mock RemoveContainerCmd removeContainerCmd;
    @Mock ListContainersCmd listContainersCmd;

    DockerRuntime runtime;
    ScalingProperties props;

    @BeforeEach
    void setUp() {
        props = new ScalingProperties();
        props.setDockerWorkerImage("cloudbalancer-worker");
        props.setDockerNetworkName("docker_default");
        props.setKafkaBootstrapInternal("kafka:29092");
        props.setDrainTimeSeconds(60);
        runtime = new DockerRuntime(dockerClient, workerRegistry, eventPublisher, props);

        // Default: reconciliation finds no containers
        lenient().when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        lenient().when(listContainersCmd.withShowAll(anyBoolean())).thenReturn(listContainersCmd);
        lenient().when(listContainersCmd.withNameFilter(any())).thenReturn(listContainersCmd);
        lenient().when(listContainersCmd.exec()).thenReturn(List.of());
    }

    @Test
    void startWorkerCreatesAndStartsContainer() {
        var createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-123");

        when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withName(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withEnv(any(String[].class))).thenReturn(createContainerCmd);
        when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenReturn(createResponse);

        when(dockerClient.startContainerCmd("container-123")).thenReturn(startContainerCmd);

        var config = new WorkerConfig("auto-docker-1", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());

        boolean result = runtime.startWorker(config);

        assertThat(result).isTrue();
        verify(dockerClient).createContainerCmd("cloudbalancer-worker");
        verify(dockerClient).startContainerCmd("container-123");
        verify(workerRegistry).registerWorker(eq("auto-docker-1"), eq(WorkerHealthState.HEALTHY), any());
        verify(eventPublisher).publishEvent(eq("workers.registration"), eq("auto-docker-1"), any());
    }

    @Test
    void stopWorkerStopsAndRemovesContainer() {
        // First start a worker to track the container
        var createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-456");
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withName(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withEnv(any(String[].class))).thenReturn(createContainerCmd);
        when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenReturn(createResponse);
        when(dockerClient.startContainerCmd("container-456")).thenReturn(startContainerCmd);

        var config = new WorkerConfig("auto-docker-2", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());
        runtime.startWorker(config);

        // Now stop
        when(dockerClient.stopContainerCmd("container-456")).thenReturn(stopContainerCmd);
        when(stopContainerCmd.withTimeout(10)).thenReturn(stopContainerCmd);
        when(dockerClient.removeContainerCmd("container-456")).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd);

        runtime.stopWorker("auto-docker-2");

        verify(dockerClient).stopContainerCmd("container-456");
        verify(dockerClient).removeContainerCmd("container-456");
    }

    @Test
    void drainAndStopPublishesDrainCommandAndSchedulesStop() throws InterruptedException {
        // Start a worker first
        var createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("container-789");
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withName(anyString())).thenReturn(createContainerCmd);
        when(createContainerCmd.withEnv(any(String[].class))).thenReturn(createContainerCmd);
        when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        when(createContainerCmd.exec()).thenReturn(createResponse);
        when(dockerClient.startContainerCmd("container-789")).thenReturn(startContainerCmd);

        var config = new WorkerConfig("auto-docker-3", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());
        runtime.startWorker(config);

        // Drain with 1 second delay for fast test
        runtime.drainAndStop("auto-docker-3", 1);

        // Verify DrainCommand published immediately
        verify(eventPublisher).publishMessage(eq("workers.commands"), eq("auto-docker-3"), any(DrainCommand.class));

        // Wait for delayed stop
        when(dockerClient.stopContainerCmd("container-789")).thenReturn(stopContainerCmd);
        when(stopContainerCmd.withTimeout(10)).thenReturn(stopContainerCmd);
        when(dockerClient.removeContainerCmd("container-789")).thenReturn(removeContainerCmd);
        when(removeContainerCmd.withForce(true)).thenReturn(removeContainerCmd);

        Thread.sleep(2000); // Wait for 1s drain + buffer

        verify(dockerClient).stopContainerCmd("container-789");
    }

    @Test
    void startWorkerReturnsFalseOnDockerError() {
        when(dockerClient.createContainerCmd(anyString())).thenThrow(new RuntimeException("Docker unavailable"));

        var config = new WorkerConfig("auto-docker-fail", Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240, Set.of());

        boolean result = runtime.startWorker(config);

        assertThat(result).isFalse();
    }

    @Test
    void stopWorkerSkipsWhenNoContainerTracked() {
        runtime.stopWorker("unknown-worker");

        verify(dockerClient, never()).stopContainerCmd(anyString());
    }
}

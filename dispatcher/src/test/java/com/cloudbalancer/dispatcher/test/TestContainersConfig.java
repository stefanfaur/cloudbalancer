package com.cloudbalancer.dispatcher.test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("cloudbalancer")
            .withUsername("postgres")
            .withPassword("postgres");

    private static final KafkaContainer KAFKA =
        new KafkaContainer("apache/kafka:3.9.0")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return KAFKA;
    }

    @Bean
    @Primary
    DockerClient dockerClient() {
        var client = mock(DockerClient.class);
        var listCmd = mock(ListContainersCmd.class);
        when(client.listContainersCmd()).thenReturn(listCmd);
        when(listCmd.withShowAll(anyBoolean())).thenReturn(listCmd);
        when(listCmd.withNameFilter(any())).thenReturn(listCmd);
        when(listCmd.exec()).thenReturn(List.of());

        var createCmd = mock(CreateContainerCmd.class);
        var createResponse = mock(CreateContainerResponse.class);
        when(createResponse.getId()).thenReturn("test-container-mock");
        when(client.createContainerCmd(anyString())).thenReturn(createCmd);
        when(createCmd.withName(anyString())).thenReturn(createCmd);
        when(createCmd.withEnv(any(String[].class))).thenReturn(createCmd);
        when(createCmd.withHostConfig(any())).thenReturn(createCmd);
        when(createCmd.exec()).thenReturn(createResponse);
        var startCmd = mock(StartContainerCmd.class);
        when(client.startContainerCmd(anyString())).thenReturn(startCmd);

        var stopCmd = mock(StopContainerCmd.class);
        when(stopCmd.withTimeout(anyInt())).thenReturn(stopCmd);
        when(client.stopContainerCmd(anyString())).thenReturn(stopCmd);
        var removeCmd = mock(RemoveContainerCmd.class);
        when(removeCmd.withForce(anyBoolean())).thenReturn(removeCmd);
        when(client.removeContainerCmd(anyString())).thenReturn(removeCmd);

        return client;
    }
}

package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.SecurityLevel;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class DockerExecutorTest {

    @org.testcontainers.junit.jupiter.Container
    static final GenericContainer<?> dind = new GenericContainer<>("docker:dind")
            .withPrivilegedMode(true)
            .withEnv("DOCKER_TLS_CERTDIR", "")
            .withExposedPorts(2375)
            .waitingFor(Wait.forListeningPort());

    static DockerClient dockerClient;
    static DockerExecutor executor;

    @BeforeAll
    static void setUp() {
        String host = "tcp://" + dind.getHost() + ":" + dind.getMappedPort(2375);
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        executor = new DockerExecutor(dockerClient);
    }

    // ---- Validation tests ----

    @Test
    void validateRejectsMissingImageField() {
        Map<String, Object> spec = Map.of("command", List.of("echo", "hello"));
        ValidationResult result = executor.validate(spec);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().get(0)).containsIgnoringCase("image");
    }

    @Test
    void validateAcceptsValidSpecWithImage() {
        Map<String, Object> spec = Map.of("image", "alpine:latest");
        ValidationResult result = executor.validate(spec);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---- Type and capabilities tests ----

    @Test
    void getExecutorTypeReturnsDocker() {
        assertThat(executor.getExecutorType()).isEqualTo(ExecutorType.DOCKER);
    }

    @Test
    void getCapabilitiesReturnsDockerRequiredAndIsolated() {
        ExecutorCapabilities capabilities = executor.getCapabilities();

        assertThat(capabilities.requiresDocker()).isTrue();
        assertThat(capabilities.securityLevel()).isEqualTo(SecurityLevel.ISOLATED);
    }

    // ---- Execution tests ----

    @Test
    void executeEchoHelloReturnsStdoutAndContainerCleanedUp(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of(
                "image", "alpine:latest",
                "command", List.of("echo", "hello")
        );
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).contains("hello");
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);

        // Verify container was cleaned up — no containers with our task label remain
        List<com.github.dockerjava.api.model.Container> remaining = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of("cloudbalancer.task-id", context.taskId().toString()))
                .exec();
        assertThat(remaining).isEmpty();
    }

    @Test
    void executeWithInvalidImageCapturesError(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of(
                "image", "this-image-does-not-exist-xyz:99.99",
                "command", List.of("echo", "hello")
        );
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.stderr()).isNotBlank();
    }

    @Test
    void executeWithNetworkDisabledCreatesContainerWithoutNetwork(@TempDir Path workDir) {
        Map<String, Object> spec = new HashMap<>();
        spec.put("image", "alpine:latest");
        spec.put("command", List.of("echo", "network-test"));
        spec.put("networkDisabled", true);

        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        // Container should still run successfully — echo doesn't need network
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).contains("network-test");
    }

    // ---- Resource estimation test ----

    @Test
    void estimateResourcesReturnsReasonableDefaults() {
        Map<String, Object> spec = Map.of("image", "alpine:latest");
        ResourceEstimate estimate = executor.estimateResources(spec);

        assertThat(estimate.estimatedCpuCores()).isEqualTo(1);
        assertThat(estimate.estimatedMemoryMB()).isEqualTo(512);
        assertThat(estimate.estimatedDurationMs()).isEqualTo(120_000L);
    }
}

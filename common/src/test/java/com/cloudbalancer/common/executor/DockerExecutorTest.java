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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    // ---- Resource limit, cancel, cleanup, and read-only rootfs tests ----

    @Test
    void executeWithMemoryLimitKillsOOMContainer(@TempDir Path workDir) {
        Map<String, Object> spec = new HashMap<>();
        spec.put("image", "alpine:latest");
        // Use a shell variable to accumulate data in memory until OOM
        // This creates a growing string in shell memory, exceeding the 64MB limit
        spec.put("command", List.of("sh", "-c",
                "x=$(cat /dev/zero | tr '\\0' 'A' | head -c 128000000); echo $x > /dev/null"));
        spec.put("memoryLimitBytes", 67108864L); // 64MB
        // Also disable swap so OOM killer triggers promptly
        spec.put("memorySwapBytes", 67108864L);

        var allocation = new ResourceAllocation(1, 64, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        // Container should be OOM-killed (exit code 137) or otherwise fail
        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isNotEqualTo(0);
    }

    @Test
    void cancelKillsRunningContainerAndReturnsNonZero(@TempDir Path workDir) throws Exception {
        Map<String, Object> spec = new HashMap<>();
        spec.put("image", "alpine:latest");
        spec.put("command", List.of("sleep", "300"));

        var allocation = new ResourceAllocation(1, 256, 100);
        UUID taskId = UUID.randomUUID();
        var context = new TaskContext(taskId, workDir);

        // Run execute in background
        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(
                () -> executor.execute(spec, allocation, context));

        // Give the container time to start
        Thread.sleep(2000);

        // Cancel the running container
        executor.cancel(new ExecutionHandle(taskId.toString()));

        // execute() should return within a reasonable time with a non-zero exit
        ExecutionResult result = future.get(30, TimeUnit.SECONDS);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isNotEqualTo(0);

        // Container should be cleaned up
        List<com.github.dockerjava.api.model.Container> remaining = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of("cloudbalancer.task-id", taskId.toString()))
                .exec();
        assertThat(remaining).isEmpty();
    }

    @Test
    void containerCleanedUpAfterBadEntrypoint(@TempDir Path workDir) {
        UUID taskId = UUID.randomUUID();
        Map<String, Object> spec = new HashMap<>();
        spec.put("image", "alpine:latest");
        spec.put("command", List.of("/nonexistent/binary"));

        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(taskId, workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        // The command should fail
        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isNotEqualTo(0);

        // No orphaned containers with our task label should remain
        List<com.github.dockerjava.api.model.Container> remaining = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of("cloudbalancer.task-id", taskId.toString()))
                .exec();
        assertThat(remaining).isEmpty();
    }

    @Test
    void executeWithReadOnlyRootfsFailsOnWrite(@TempDir Path workDir) {
        Map<String, Object> spec = new HashMap<>();
        spec.put("image", "alpine:latest");
        spec.put("command", List.of("sh", "-c", "echo test > /tmp/file"));
        spec.put("readOnlyRootfs", true);

        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        // Write to read-only filesystem should fail
        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isNotEqualTo(0);
    }
}

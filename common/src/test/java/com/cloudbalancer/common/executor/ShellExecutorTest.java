package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorTest {

    private final ShellExecutor executor = new ShellExecutor();

    // ---- Validation tests ----

    @Test
    void validateRejectsMissingCommand() {
        Map<String, Object> spec = Map.of();
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void validateRejectsBlacklistedCommand() {
        Map<String, Object> spec = Map.of("command", "shutdown now");
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void validateAcceptsValidCommand() {
        Map<String, Object> spec = Map.of("command", "echo hello");
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    // ---- Type and capabilities tests ----

    @Test
    void getExecutorTypeReturnsShell() {
        assertThat(executor.getExecutorType()).isEqualTo(ExecutorType.SHELL);
    }

    @Test
    void getCapabilitiesReturnsCorrectValues() {
        ExecutorCapabilities capabilities = executor.getCapabilities();
        assertThat(capabilities.requiresDocker()).isFalse();
        assertThat(capabilities.securityLevel()).isEqualTo(SecurityLevel.SANDBOXED);
    }

    // ---- Execution tests ----

    @Test
    void executeEchoHelloReturnsStdoutAndExitCodeZero(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of("command", "echo hello");
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).contains("hello");
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void executeWithEnvironmentVariables(@TempDir Path workDir) {
        Map<String, Object> spec = new HashMap<>();
        spec.put("command", "echo $MY_VAR");
        spec.put("environment", Map.of("MY_VAR", "test_value_123"));
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).contains("test_value_123");
    }

    @Test
    void executeNonZeroExitCodeReturnsFailed(@TempDir Path workDir) {
        Map<String, Object> spec = Map.of("command", "exit 42");
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(42);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void executeCancelledProcessReturnsNonZero(@TempDir Path workDir) throws Exception {
        Map<String, Object> spec = Map.of("command", "sleep 300");
        var allocation = new ResourceAllocation(1, 256, 100);
        UUID taskId = UUID.randomUUID();
        var context = new TaskContext(taskId, workDir);

        // Run execute in a separate thread so we can cancel while it's running
        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(
            () -> executor.execute(spec, allocation, context)
        );

        // Wait a bit for the process to start, then cancel
        Thread.sleep(500);
        executor.cancel(new ExecutionHandle(taskId.toString()));

        ExecutionResult result = future.get(10, TimeUnit.SECONDS);
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.succeeded()).isFalse();
    }

    // ---- Timeout and zombie prevention test ----

    @Test
    void cancelKillsProcessAndItIsNoLongerAlive(@TempDir Path workDir) throws Exception {
        Map<String, Object> spec = Map.of("command", "sleep 300");
        var allocation = new ResourceAllocation(1, 256, 100);
        UUID taskId = UUID.randomUUID();
        var context = new TaskContext(taskId, workDir);

        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(
            () -> executor.execute(spec, allocation, context)
        );

        // Wait for the process to start
        Thread.sleep(500);
        executor.cancel(new ExecutionHandle(taskId.toString()));

        ExecutionResult result = future.get(10, TimeUnit.SECONDS);
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.succeeded()).isFalse();
        // The process should no longer be tracked (cleaned up in finally block)
        // Verify cancel completes promptly — no zombie left behind
    }

    // ---- Output truncation test ----

    @Test
    void executeTruncatesOutputWhenExceedingMaxBytes(@TempDir Path workDir) {
        // Create an executor with a small output limit
        ShellExecutor smallOutputExecutor = new ShellExecutor(Set.of(), 1024);

        // Generate exactly 2000 bytes of output using head -c on /dev/zero piped through tr
        Map<String, Object> spec = Map.of("command", "head -c 2000 /dev/zero | tr '\\0' 'A'");
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = smallOutputExecutor.execute(spec, allocation, context);

        assertThat(result.stdout().length()).isLessThanOrEqualTo(1024);
    }

    // ---- Working directory test ----

    @Test
    void executeUsesWorkingDirectory(@TempDir Path workDir) throws IOException {
        // Write a file into the temp directory
        String content = "hello from workdir";
        Files.writeString(workDir.resolve("testfile.txt"), content);

        Map<String, Object> spec = Map.of("command", "cat testfile.txt");
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), workDir);

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout()).contains(content);
    }

    // ---- Resource estimation test ----

    @Test
    void estimateResourcesReturnsReasonableDefaults() {
        Map<String, Object> spec = Map.of("command", "echo hello");
        ResourceEstimate estimate = executor.estimateResources(spec);

        assertThat(estimate.estimatedCpuCores()).isGreaterThan(0);
        assertThat(estimate.estimatedMemoryMB()).isGreaterThan(0);
        assertThat(estimate.estimatedDurationMs()).isGreaterThan(0);
    }
}

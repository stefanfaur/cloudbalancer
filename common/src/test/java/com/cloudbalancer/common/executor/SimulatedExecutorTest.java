package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class SimulatedExecutorTest {

    private final SimulatedExecutor executor = new SimulatedExecutor();

    @Test
    void validateAcceptsValidSpec() {
        Map<String, Object> spec = Map.of("durationMs", 5000, "failProbability", 0.1);
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validateRejectsNegativeDuration() {
        Map<String, Object> spec = Map.of("durationMs", -100);
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void validateRejectsMissingDuration() {
        Map<String, Object> spec = Map.of("failProbability", 0.5);
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateRejectsInvalidFailProbability() {
        Map<String, Object> spec = Map.of("durationMs", 1000, "failProbability", 1.5);
        ValidationResult result = executor.validate(spec);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void getCapabilitiesReturnsCorrectValues() {
        ExecutorCapabilities capabilities = executor.getCapabilities();
        assertThat(capabilities.requiresDocker()).isFalse();
        assertThat(capabilities.requiresNetworkAccess()).isFalse();
        assertThat(capabilities.securityLevel()).isEqualTo(SecurityLevel.SANDBOXED);
    }

    @Test
    void estimateResourcesBasedOnSpec() {
        Map<String, Object> spec = Map.of("durationMs", 5000, "cpuIntensity", 0.8);
        ResourceEstimate estimate = executor.estimateResources(spec);
        assertThat(estimate.estimatedDurationMs()).isGreaterThan(0);
        assertThat(estimate.estimatedMemoryMB()).isGreaterThan(0);
    }

    @Test
    void executorTypeIsSimulated() {
        assertThat(executor.getExecutorType()).isEqualTo(ExecutorType.SIMULATED);
    }

    @Test
    void executeSleepsForConfiguredDuration() {
        Map<String, Object> spec = Map.of("durationMs", 500, "failProbability", 0.0);
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), Path.of("/tmp"));

        long start = System.currentTimeMillis();
        ExecutionResult result = executor.execute(spec, allocation, context);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(400); // allow timing slack
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void executeAlwaysFailsWithProbabilityOne() {
        Map<String, Object> spec = Map.of("durationMs", 100, "failProbability", 1.0);
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), Path.of("/tmp"));

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.stderr()).isNotEmpty();
    }

    @Test
    void executeAlwaysSucceedsWithProbabilityZero() {
        Map<String, Object> spec = Map.of("durationMs", 100, "failProbability", 0.0);
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), Path.of("/tmp"));

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void executeReportsActualDuration() {
        Map<String, Object> spec = Map.of("durationMs", 300, "failProbability", 0.0);
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), Path.of("/tmp"));

        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.durationMs()).isGreaterThanOrEqualTo(250);
    }

    @Test
    void executeHandlesInterruptAsTimeout() {
        Map<String, Object> spec = Map.of("durationMs", 10000, "failProbability", 0.0);
        var allocation = new ResourceAllocation(1, 256, 100);
        var context = new TaskContext(UUID.randomUUID(), Path.of("/tmp"));

        Thread.currentThread().interrupt();
        ExecutionResult result = executor.execute(spec, allocation, context);

        assertThat(result.timedOut()).isTrue();
        assertThat(result.succeeded()).isFalse();
    }
}

package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
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
}

package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class WorkerInfoTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void workerInfoRoundTrip() throws Exception {
        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL),
            new ResourceProfile(8, 16384, 51200, false, 0, true),
            Set.of("region-us")
        );
        var metrics = new WorkerMetrics(45.5, 2048, 4096, 16, 3, 100L, 2L, 250.0, Instant.now());
        var info = new WorkerInfo("worker-1", WorkerHealthState.HEALTHY, capabilities, metrics, Instant.now());

        String json = mapper.writeValueAsString(info);
        WorkerInfo deserialized = mapper.readValue(json, WorkerInfo.class);

        assertThat(deserialized.id()).isEqualTo("worker-1");
        assertThat(deserialized.healthState()).isEqualTo(WorkerHealthState.HEALTHY);
        assertThat(deserialized.capabilities().supportedExecutors())
            .containsExactlyInAnyOrder(ExecutorType.SIMULATED, ExecutorType.SHELL);
        assertThat(deserialized.capabilities().tags()).contains("region-us");
    }

    @Test
    void executorCapabilitiesRoundTrip() throws Exception {
        var cap = new ExecutorCapabilities(
            false, false,
            new ResourceProfile(4, 8192, 10240, false, 3600, false),
            SecurityLevel.SANDBOXED
        );

        String json = mapper.writeValueAsString(cap);
        ExecutorCapabilities deserialized = mapper.readValue(json, ExecutorCapabilities.class);

        assertThat(deserialized.requiresDocker()).isFalse();
        assertThat(deserialized.securityLevel()).isEqualTo(SecurityLevel.SANDBOXED);
    }

    @Test
    void workerCapabilitiesSupportsExecutor() {
        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of()
        );

        assertThat(capabilities.supportsExecutor(ExecutorType.SIMULATED)).isTrue();
        assertThat(capabilities.supportsExecutor(ExecutorType.DOCKER)).isFalse();
    }

    @Test
    void workerMetricsRoundTrip() throws Exception {
        var metrics = new WorkerMetrics(88.5, 3072, 8192, 32, 5, 200L, 3L, 180.0, Instant.now());

        String json = mapper.writeValueAsString(metrics);
        WorkerMetrics deserialized = mapper.readValue(json, WorkerMetrics.class);

        assertThat(deserialized.cpuUsagePercent()).isEqualTo(88.5);
        assertThat(deserialized.activeTaskCount()).isEqualTo(5);
    }
}

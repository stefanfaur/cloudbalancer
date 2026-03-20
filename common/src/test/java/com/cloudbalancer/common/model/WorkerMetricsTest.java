package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class WorkerMetricsTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void serializationRoundTrip() throws Exception {
        var now = Instant.now();
        var metrics = new WorkerMetrics(
            75.5,   // cpuUsagePercent
            2048,   // heapUsedMB
            4096,   // heapMaxMB
            42,     // threadCount
            5,      // activeTaskCount
            1000L,  // completedTaskCount
            12L,    // failedTaskCount
            250.5,  // averageExecutionDurationMs
            now     // reportedAt
        );

        String json = mapper.writeValueAsString(metrics);
        WorkerMetrics deserialized = mapper.readValue(json, WorkerMetrics.class);

        assertThat(deserialized.cpuUsagePercent()).isEqualTo(75.5);
        assertThat(deserialized.heapUsedMB()).isEqualTo(2048);
        assertThat(deserialized.heapMaxMB()).isEqualTo(4096);
        assertThat(deserialized.threadCount()).isEqualTo(42);
        assertThat(deserialized.activeTaskCount()).isEqualTo(5);
        assertThat(deserialized.completedTaskCount()).isEqualTo(1000L);
        assertThat(deserialized.failedTaskCount()).isEqualTo(12L);
        assertThat(deserialized.averageExecutionDurationMs()).isEqualTo(250.5);
        assertThat(deserialized.reportedAt()).isEqualTo(now);
    }

    @Test
    void jsonContainsExpectedFieldNames() throws Exception {
        var metrics = new WorkerMetrics(
            50.0, 1024, 2048, 20, 3, 500L, 5L, 100.0, Instant.now()
        );

        String json = mapper.writeValueAsString(metrics);

        assertThat(json).contains("\"cpuUsagePercent\"");
        assertThat(json).contains("\"heapUsedMB\"");
        assertThat(json).contains("\"heapMaxMB\"");
        assertThat(json).contains("\"threadCount\"");
        assertThat(json).contains("\"activeTaskCount\"");
        assertThat(json).contains("\"completedTaskCount\"");
        assertThat(json).contains("\"failedTaskCount\"");
        assertThat(json).contains("\"averageExecutionDurationMs\"");
        assertThat(json).contains("\"reportedAt\"");
    }
}

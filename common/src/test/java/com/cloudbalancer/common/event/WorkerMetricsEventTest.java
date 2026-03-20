package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.WorkerMetrics;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class WorkerMetricsEventTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void polymorphicSerializationRoundTrip() throws Exception {
        var now = Instant.now();
        var metrics = new WorkerMetrics(
            82.3,   // cpuUsagePercent
            3072,   // heapUsedMB
            8192,   // heapMaxMB
            64,     // threadCount
            8,      // activeTaskCount
            5000L,  // completedTaskCount
            25L,    // failedTaskCount
            320.7,  // averageExecutionDurationMs
            now     // reportedAt
        );

        var event = new WorkerMetricsEvent(
            UUID.randomUUID().toString(),
            now,
            "worker-42",
            metrics
        );

        // Serialize as CloudBalancerEvent (polymorphic)
        String json = mapper.writeValueAsString(event);

        // Deserialize back as CloudBalancerEvent
        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);

        assertThat(deserialized).isInstanceOf(WorkerMetricsEvent.class);
        WorkerMetricsEvent typed = (WorkerMetricsEvent) deserialized;

        assertThat(typed.eventType()).isEqualTo("WORKER_METRICS");
        assertThat(typed.workerId()).isEqualTo("worker-42");

        // Assert all nested WorkerMetrics fields survive round-trip
        WorkerMetrics m = typed.metrics();
        assertThat(m.cpuUsagePercent()).isEqualTo(82.3);
        assertThat(m.heapUsedMB()).isEqualTo(3072);
        assertThat(m.heapMaxMB()).isEqualTo(8192);
        assertThat(m.threadCount()).isEqualTo(64);
        assertThat(m.activeTaskCount()).isEqualTo(8);
        assertThat(m.completedTaskCount()).isEqualTo(5000L);
        assertThat(m.failedTaskCount()).isEqualTo(25L);
        assertThat(m.averageExecutionDurationMs()).isEqualTo(320.7);
        assertThat(m.reportedAt()).isEqualTo(now);
    }

    @Test
    void jsonContainsEventTypeDiscriminator() throws Exception {
        var event = new WorkerMetricsEvent(
            "evt-1", Instant.now(), "worker-1",
            new WorkerMetrics(0, 0, 0, 0, 0, 0L, 0L, 0.0, Instant.now())
        );

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"WORKER_METRICS\"");
    }
}

package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.WorkerMetrics;
import java.time.Instant;

public record WorkerMetricsEvent(
    String eventId,
    Instant timestamp,
    String workerId,
    WorkerMetrics metrics
) implements CloudBalancerEvent {
    @Override public String eventType() { return "WORKER_METRICS"; }
}

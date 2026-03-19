package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.WorkerCapabilities;
import java.time.Instant;

public record WorkerRegisteredEvent(
    String eventId,
    Instant timestamp,
    String workerId,
    WorkerCapabilities capabilities
) implements CloudBalancerEvent {
    @Override public String eventType() { return "WORKER_REGISTERED"; }
}

package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.WorkerHealthState;
import java.time.Instant;

public record WorkerHeartbeatEvent(
    String eventId,
    Instant timestamp,
    String workerId,
    WorkerHealthState healthState
) implements CloudBalancerEvent {
    @Override public String eventType() { return "WORKER_HEARTBEAT"; }
}

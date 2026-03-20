package com.cloudbalancer.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignedEvent(
    String eventId,
    Instant timestamp,
    UUID taskId,
    String workerId
) implements CloudBalancerEvent {
    @Override public String eventType() { return "TASK_ASSIGNED"; }
}

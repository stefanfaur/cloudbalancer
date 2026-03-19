package com.cloudbalancer.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCompletedEvent(
    String eventId,
    Instant timestamp,
    UUID taskId,
    int exitCode,
    String stdout,
    String stderr
) implements CloudBalancerEvent {
    @Override public String eventType() { return "TASK_COMPLETED"; }
}

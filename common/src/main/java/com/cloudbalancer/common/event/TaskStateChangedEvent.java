package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.TaskState;
import java.time.Instant;
import java.util.UUID;

public record TaskStateChangedEvent(
    String eventId,
    Instant timestamp,
    UUID taskId,
    TaskState previousState,
    TaskState newState,
    String reason
) implements CloudBalancerEvent {
    @Override public String eventType() { return "TASK_STATE_CHANGED"; }
}

package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.TaskDescriptor;
import java.time.Instant;
import java.util.UUID;

public record TaskSubmittedEvent(
    String eventId,
    Instant timestamp,
    UUID taskId,
    TaskDescriptor descriptor
) implements CloudBalancerEvent {
    @Override public String eventType() { return "TASK_SUBMITTED"; }
}

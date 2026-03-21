package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.ExecutionAttempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskDeadLetteredEvent(
    String eventId,
    Instant timestamp,
    UUID taskId,
    String reason,
    int attemptCount,
    List<ExecutionAttempt> executionHistory
) implements CloudBalancerEvent {
    @Override
    public String eventType() { return "TASK_DEAD_LETTERED"; }
}

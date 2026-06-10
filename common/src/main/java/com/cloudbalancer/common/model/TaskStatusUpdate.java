package com.cloudbalancer.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * An in-progress status signal published by a worker as it begins executing a
 * task, ahead of the terminal {@link TaskResult}. It lets the dispatcher move a
 * task out of ASSIGNED promptly (so the stale-assignment scanner stops treating
 * a live, executing task as a lost assignment). The {@code executionId} ties the
 * update to a specific execution attempt so the dispatcher can discard stale
 * signals from superseded attempts.
 */
public record TaskStatusUpdate(
    UUID taskId,
    String workerId,
    UUID executionId,
    TaskState state,
    Instant timestamp
) {}

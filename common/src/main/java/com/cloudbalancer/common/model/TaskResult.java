package com.cloudbalancer.common.model;

import java.time.Instant;
import java.util.UUID;

public record TaskResult(
    UUID taskId,
    String workerId,
    int exitCode,
    String stdout,
    String stderr,
    long executionDurationMs,
    boolean timedOut,
    Instant completedAt,
    UUID executionId
) {
    public boolean succeeded() { return exitCode == 0 && !timedOut; }
}

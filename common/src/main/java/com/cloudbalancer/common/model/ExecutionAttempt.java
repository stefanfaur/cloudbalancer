package com.cloudbalancer.common.model;

import java.time.Instant;
import java.util.UUID;

public record ExecutionAttempt(
    int attemptNumber,
    String workerId,
    Instant startedAt,
    Instant completedAt,
    int exitCode,
    ResourceProfile actualResources,
    String failureReason,
    boolean workerCausedFailure,
    UUID executionId
) {}

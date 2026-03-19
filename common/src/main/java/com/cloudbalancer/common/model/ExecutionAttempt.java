package com.cloudbalancer.common.model;

import java.time.Instant;

public record ExecutionAttempt(
    int attemptNumber,
    String workerId,
    Instant startedAt,
    Instant completedAt,
    int exitCode,
    ResourceProfile actualResources
) {}

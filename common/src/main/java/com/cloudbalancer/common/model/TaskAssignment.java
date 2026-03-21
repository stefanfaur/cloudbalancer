package com.cloudbalancer.common.model;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignment(
    UUID taskId,
    TaskDescriptor descriptor,
    String assignedWorkerId,
    Instant assignedAt,
    UUID executionId
) {}

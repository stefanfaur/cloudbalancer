package com.cloudbalancer.common.model;

import java.time.Instant;

public record WorkerInfo(
    String id,
    WorkerHealthState healthState,
    WorkerCapabilities capabilities,
    WorkerMetrics currentMetrics,
    Instant registeredAt
) {}

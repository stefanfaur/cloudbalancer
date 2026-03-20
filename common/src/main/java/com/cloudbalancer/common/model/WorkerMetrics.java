package com.cloudbalancer.common.model;

import java.time.Instant;

public record WorkerMetrics(
    double cpuUsagePercent,
    long heapUsedMB,
    long heapMaxMB,
    int threadCount,
    int activeTaskCount,
    long completedTaskCount,
    long failedTaskCount,
    double averageExecutionDurationMs,
    Instant reportedAt
) {}

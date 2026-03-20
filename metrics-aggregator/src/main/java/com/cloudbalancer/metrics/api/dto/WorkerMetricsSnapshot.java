package com.cloudbalancer.metrics.api.dto;

import java.time.Instant;

/**
 * Latest metrics snapshot for a single worker.
 */
public record WorkerMetricsSnapshot(
        String workerId,
        double cpuUsagePercent,
        long heapUsedMB,
        long heapMaxMB,
        int threadCount,
        int activeTaskCount,
        long completedTaskCount,
        long failedTaskCount,
        double avgExecutionDurationMs,
        Instant reportedAt
) {}

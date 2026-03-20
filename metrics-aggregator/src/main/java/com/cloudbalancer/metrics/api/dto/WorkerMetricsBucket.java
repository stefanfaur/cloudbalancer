package com.cloudbalancer.metrics.api.dto;

import java.time.Instant;

/**
 * Aggregated metrics for a time bucket.
 */
public record WorkerMetricsBucket(
        Instant bucketStart,
        String workerId,
        double avgCpuPercent,
        long avgHeapUsedMB,
        long avgHeapMaxMB,
        int avgThreadCount,
        int avgActiveTaskCount,
        long avgCompletedTaskCount,
        long avgFailedTaskCount,
        double avgExecutionDurationMs
) {}

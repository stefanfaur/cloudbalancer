package com.cloudbalancer.metrics.api.dto;

/**
 * Aggregate cluster-wide metrics computed from latest-per-worker and task_metrics.
 */
public record ClusterMetrics(
        double avgCpuPercent,
        int totalActiveTaskCount,
        long totalHeapUsedMB,
        double throughputPerMinute,
        double avgQueueWaitMs,
        double avgExecutionDurationMs,
        int workerCount,
        int healthyWorkerCount
) {}

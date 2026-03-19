package com.cloudbalancer.common.model;

public record WorkerMetrics(
    double cpuUtilization,
    double memoryUsagePercent,
    int activeTaskCount,
    int queuedTaskCount,
    double taskCompletionRate,
    double averageTaskLatencyMs
) {}

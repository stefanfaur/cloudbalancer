package com.cloudbalancer.common.runtime;

import com.cloudbalancer.common.model.ExecutorType;
import java.util.Set;

public record WorkerConfig(
    String workerId,
    Set<ExecutorType> supportedExecutors,
    int cpuCores,
    int memoryMB,
    int diskMB,
    Set<String> tags
) {}

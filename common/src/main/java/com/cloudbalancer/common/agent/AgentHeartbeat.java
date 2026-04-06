package com.cloudbalancer.common.agent;

import com.cloudbalancer.common.model.ExecutorType;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record AgentHeartbeat(
    String agentId,
    String hostname,
    double totalCpuCores,
    double availableCpuCores,
    long totalMemoryMB,
    long availableMemoryMB,
    List<String> activeWorkerIds,
    Set<ExecutorType> supportedExecutors,
    Instant timestamp
) {}

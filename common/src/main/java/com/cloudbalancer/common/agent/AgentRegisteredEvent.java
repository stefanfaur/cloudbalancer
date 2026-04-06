package com.cloudbalancer.common.agent;

import com.cloudbalancer.common.model.ExecutorType;
import java.time.Instant;
import java.util.Set;

public record AgentRegisteredEvent(
    String agentId,
    String hostname,
    double totalCpuCores,
    long totalMemoryMB,
    Set<ExecutorType> supportedExecutors,
    Instant timestamp
) {}

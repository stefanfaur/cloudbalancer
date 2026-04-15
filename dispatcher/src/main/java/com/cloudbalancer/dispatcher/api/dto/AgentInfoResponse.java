package com.cloudbalancer.dispatcher.api.dto;

import java.util.List;

public record AgentInfoResponse(
    String agentId, String hostname,
    double totalCpuCores, double availableCpuCores,
    long totalMemoryMB, long availableMemoryMB,
    List<String> activeWorkerIds,
    List<String> supportedExecutors,
    String lastHeartbeat
) {}

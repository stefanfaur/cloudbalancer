package com.cloudbalancer.dispatcher.api.dto;

import com.cloudbalancer.common.model.ScalingDecision;
import com.cloudbalancer.common.model.ScalingPolicy;

public record ScalingStatusResponse(
    int workerCount,
    int activeWorkerCount,
    int drainingWorkerCount,
    ScalingPolicy policy,
    ScalingDecision lastDecision,
    long cooldownRemainingSeconds,
    String runtimeMode
) {}

package com.cloudbalancer.common.model;

import java.time.Instant;

public record ScalingDecision(
    ScalingAction action,
    String reason,
    ScalingTriggerType triggerType,
    int previousWorkerCount,
    int newWorkerCount,
    Instant timestamp
) {}

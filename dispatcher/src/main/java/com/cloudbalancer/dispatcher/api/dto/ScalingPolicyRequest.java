package com.cloudbalancer.dispatcher.api.dto;

public record ScalingPolicyRequest(
    int minWorkers,
    int maxWorkers,
    int cooldownSeconds,
    int scaleUpStep,
    int scaleDownStep,
    int drainTimeSeconds
) {}

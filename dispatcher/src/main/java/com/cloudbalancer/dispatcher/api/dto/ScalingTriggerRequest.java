package com.cloudbalancer.dispatcher.api.dto;

public record ScalingTriggerRequest(
    String action,
    int count,
    String agentId
) {}

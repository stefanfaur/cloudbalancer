package com.cloudbalancer.dispatcher.api.dto;

import com.cloudbalancer.common.model.ScalingPolicy;
import java.time.Instant;

public record ScalingPolicyResponse(
    ScalingPolicy policy,
    Instant updatedAt
) {}

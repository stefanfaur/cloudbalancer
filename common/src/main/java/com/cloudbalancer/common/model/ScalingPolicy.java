package com.cloudbalancer.common.model;

import java.time.Duration;

public record ScalingPolicy(
    int minWorkers,
    int maxWorkers,
    Duration cooldownPeriod,
    int scaleUpStep,
    int scaleDownStep,
    Duration scaleDownDrainTime
) {
    public static ScalingPolicy defaults() {
        return new ScalingPolicy(2, 20, Duration.ofMinutes(3), 1, 1, Duration.ofSeconds(60));
    }

    public static ScalingPolicy validated(int minWorkers, int maxWorkers, Duration cooldownPeriod,
                                          int scaleUpStep, int scaleDownStep, Duration scaleDownDrainTime) {
        if (minWorkers > maxWorkers)
            throw new IllegalArgumentException("minWorkers must be <= maxWorkers");
        if (cooldownPeriod.isNegative() || cooldownPeriod.isZero())
            throw new IllegalArgumentException("cooldownPeriod must be positive");
        if (scaleUpStep < 1 || scaleUpStep > 3)
            throw new IllegalArgumentException("scaleUpStep must be between 1 and 3");
        if (scaleDownStep < 1)
            throw new IllegalArgumentException("scaleDownStep must be >= 1");
        if (scaleDownDrainTime.isNegative() || scaleDownDrainTime.isZero())
            throw new IllegalArgumentException("scaleDownDrainTime must be positive");
        return new ScalingPolicy(minWorkers, maxWorkers, cooldownPeriod, scaleUpStep, scaleDownStep, scaleDownDrainTime);
    }
}

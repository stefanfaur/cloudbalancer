package com.cloudbalancer.common.model;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScalingPolicyTest {

    @Test
    void defaultsAreCorrect() {
        var policy = ScalingPolicy.defaults();
        assertThat(policy.minWorkers()).isEqualTo(2);
        assertThat(policy.maxWorkers()).isEqualTo(20);
        assertThat(policy.cooldownPeriod()).isEqualTo(Duration.ofMinutes(3));
        assertThat(policy.scaleUpStep()).isEqualTo(1);
        assertThat(policy.scaleDownStep()).isEqualTo(1);
        assertThat(policy.scaleDownDrainTime()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void validateRejectsMinGreaterThanMax() {
        assertThatThrownBy(() -> ScalingPolicy.validated(10, 5, Duration.ofMinutes(3), 1, 1, Duration.ofSeconds(60)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minWorkers must be <= maxWorkers");
    }

    @Test
    void validateRejectsNegativeCooldown() {
        assertThatThrownBy(() -> ScalingPolicy.validated(2, 20, Duration.ofSeconds(-1), 1, 1, Duration.ofSeconds(60)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cooldownPeriod must be positive");
    }

    @Test
    void validateRejectsStepOutOfRange() {
        assertThatThrownBy(() -> ScalingPolicy.validated(2, 20, Duration.ofMinutes(3), 5, 1, Duration.ofSeconds(60)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scaleUpStep must be between 1 and 3");
    }
}

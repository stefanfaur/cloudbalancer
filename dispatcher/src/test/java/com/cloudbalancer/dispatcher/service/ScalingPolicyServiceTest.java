package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.ScalingPolicy;
import com.cloudbalancer.dispatcher.persistence.ScalingPolicyRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class ScalingPolicyServiceTest {

    @Autowired ScalingPolicyService scalingPolicyService;
    @Autowired ScalingPolicyRepository scalingPolicyRepository;

    @BeforeEach
    void cleanUp() {
        scalingPolicyRepository.deleteAll();
        scalingPolicyService.reloadPolicy();
    }

    @Test
    void returnsDefaultsWhenNoDbRow() {
        var policy = scalingPolicyService.getCurrentPolicy();
        assertThat(policy.minWorkers()).isEqualTo(2);
        assertThat(policy.maxWorkers()).isEqualTo(20);
    }

    @Test
    void updatePolicyPersistsAndReturns() {
        var updated = new ScalingPolicy(3, 10, Duration.ofMinutes(5), 2, 1, Duration.ofSeconds(30));
        scalingPolicyService.updatePolicy(updated);

        var loaded = scalingPolicyService.getCurrentPolicy();
        assertThat(loaded.minWorkers()).isEqualTo(3);
        assertThat(loaded.maxWorkers()).isEqualTo(10);
        assertThat(loaded.scaleUpStep()).isEqualTo(2);
    }
}

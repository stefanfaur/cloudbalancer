package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class SchedulingConfigRepositoryTest {

    @Autowired
    SchedulingConfigRepository schedulingConfigRepository;

    @BeforeEach
    void cleanUp() {
        schedulingConfigRepository.deleteAll();
    }

    @Test
    void persistAndRetrieveWithWeights() {
        var config = new SchedulingConfigRecord("RESOURCE_FIT",
            Map.of("resourceAvailability", 80, "queueDepth", 20));
        schedulingConfigRepository.save(config);

        var all = schedulingConfigRepository.findAll();
        assertThat(all).hasSize(1);
        var found = all.getFirst();
        assertThat(found.getStrategyName()).isEqualTo("RESOURCE_FIT");
        assertThat(found.getWeights()).containsEntry("resourceAvailability", 80);
        assertThat(found.getWeights()).containsEntry("queueDepth", 20);
    }

    @Test
    void updateStrategyConfig() {
        var config = new SchedulingConfigRecord("ROUND_ROBIN", Map.of());
        schedulingConfigRepository.save(config);

        config.setStrategyName("LEAST_CONNECTIONS");
        config.setWeights(Map.of("queueDepth", 100));
        schedulingConfigRepository.save(config);

        var found = schedulingConfigRepository.findById(config.getId()).orElseThrow();
        assertThat(found.getStrategyName()).isEqualTo("LEAST_CONNECTIONS");
        assertThat(found.getWeights()).containsEntry("queueDepth", 100);
    }
}

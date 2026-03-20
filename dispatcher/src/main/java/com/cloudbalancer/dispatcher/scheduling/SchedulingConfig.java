package com.cloudbalancer.dispatcher.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SchedulingConfig {

    @Bean
    List<WorkerFilter> workerFilters() {
        return List.of(
            new HealthFilter(),
            new ExecutorCapabilityFilter(),
            new ResourceSufficiencyFilter(),
            new ConstraintFilter()
        );
    }

    @Bean
    List<WorkerScorer> workerScorers() {
        return List.of(
            new ResourceAvailabilityScorer(),
            new QueueDepthScorer()
        );
    }
}

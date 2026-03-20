package com.cloudbalancer.dispatcher.scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrategyTest {

    private final Map<String, WorkerScorer> scorers = Map.of(
        "resourceAvailability", new ResourceAvailabilityScorer(),
        "queueDepth", new QueueDepthScorer()
    );

    @Test
    void roundRobinRotatesThroughCandidates() {
        var strategy = new RoundRobinStrategy();
        var w1 = workerRecord("w1");
        var w2 = workerRecord("w2");
        var task = anyTask();

        var selected = IntStream.range(0, 4)
            .mapToObj(i -> strategy.select(task, List.of(w1, w2), scorers).orElseThrow().getId())
            .toList();

        // Should alternate: w1, w2, w1, w2
        assertThat(selected).containsExactly("w1", "w2", "w1", "w2");
    }

    @Test
    void roundRobinReturnsEmptyForNoCandidates() {
        var strategy = new RoundRobinStrategy();
        assertThat(strategy.select(anyTask(), List.of(), scorers)).isEmpty();
    }

    @Test
    void weightedRoundRobinFavorsLargerWorkers() {
        var strategy = new WeightedRoundRobinStrategy();
        // w1: 16 cpu, 8192 mem, 2000 disk → higher capacity weight
        var w1 = workerWithCapacity("w1", 16, 8192, 2000, 0, 0, 0);
        // w2: 2 cpu, 512 mem, 100 disk → lower capacity weight
        var w2 = workerWithCapacity("w2", 2, 512, 100, 0, 0, 0);
        var task = anyTask();

        long w1Count = IntStream.range(0, 100)
            .mapToObj(i -> strategy.select(task, List.of(w1, w2), scorers).orElseThrow().getId())
            .filter("w1"::equals)
            .count();

        // w1 should get significantly more selections
        assertThat(w1Count).isGreaterThan(60);
    }

    @Test
    void leastConnectionsSelectsWorkerWithFewestTasks() {
        var strategy = new LeastConnectionsStrategy();
        var busy = workerWithCapacity("busy", 8, 4096, 1000, 0, 0, 0);
        // Simulate busy worker having active tasks
        for (int i = 0; i < 10; i++) {
            busy.allocateResources(new com.cloudbalancer.common.model.ResourceProfile(0, 0, 0, false, 0, false));
        }
        var idle = workerWithCapacity("idle", 8, 4096, 1000, 0, 0, 0);

        var selected = strategy.select(anyTask(), List.of(busy, idle), scorers);
        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo("idle");
    }

    @Test
    void resourceFitSelectsBestResourceMatch() {
        var strategy = new ResourceFitStrategy();
        var loaded = workerWithCapacity("loaded", 8, 4096, 1000, 6, 3000, 800);
        var free = workerWithCapacity("free", 8, 4096, 1000, 1, 256, 50);

        var selected = strategy.select(anyTask(), List.of(loaded, free), scorers);
        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo("free");
    }

    @Test
    void customStrategyUsesProvidedWeights() {
        // Custom: 100% resource availability, 0% queue depth
        var strategy = new CustomStrategy(Map.of("resourceAvailability", 100, "queueDepth", 0));
        var loaded = workerWithCapacity("loaded", 8, 4096, 1000, 6, 3000, 800);
        var free = workerWithCapacity("free", 8, 4096, 1000, 0, 0, 0);

        var selected = strategy.select(anyTask(), List.of(loaded, free), scorers);
        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo("free");
    }

    @Test
    void factoryCreatesAllStrategies() {
        assertThat(SchedulingStrategyFactory.create("ROUND_ROBIN", Map.of()))
            .isInstanceOf(RoundRobinStrategy.class);
        assertThat(SchedulingStrategyFactory.create("WEIGHTED_ROUND_ROBIN", Map.of()))
            .isInstanceOf(WeightedRoundRobinStrategy.class);
        assertThat(SchedulingStrategyFactory.create("LEAST_CONNECTIONS", Map.of()))
            .isInstanceOf(LeastConnectionsStrategy.class);
        assertThat(SchedulingStrategyFactory.create("RESOURCE_FIT", Map.of()))
            .isInstanceOf(ResourceFitStrategy.class);
        assertThat(SchedulingStrategyFactory.create("CUSTOM", Map.of("resourceAvailability", 50)))
            .isInstanceOf(CustomStrategy.class);
    }

    @Test
    void factoryRejectsUnknownStrategy() {
        assertThatThrownBy(() -> SchedulingStrategyFactory.create("NONEXISTENT", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weightedScoringReturnsEmptyForNoCandidates() {
        var strategy = new ResourceFitStrategy();
        assertThat(strategy.select(anyTask(), List.of(), scorers)).isEmpty();
    }

    @Test
    void strategyNamesAndWeightsCorrect() {
        assertThat(new RoundRobinStrategy().getName()).isEqualTo("ROUND_ROBIN");
        assertThat(new RoundRobinStrategy().getWeights()).isEmpty();
        assertThat(new LeastConnectionsStrategy().getName()).isEqualTo("LEAST_CONNECTIONS");
        assertThat(new LeastConnectionsStrategy().getWeights()).containsEntry("queueDepth", 100);
        assertThat(new ResourceFitStrategy().getName()).isEqualTo("RESOURCE_FIT");
        assertThat(new ResourceFitStrategy().getWeights()).containsKeys("resourceAvailability", "queueDepth");
    }
}

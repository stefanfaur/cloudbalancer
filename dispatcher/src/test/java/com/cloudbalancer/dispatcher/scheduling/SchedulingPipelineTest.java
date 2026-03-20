package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.SchedulingConfigRecord;
import com.cloudbalancer.dispatcher.persistence.SchedulingConfigRepository;
import com.cloudbalancer.dispatcher.service.SchedulingConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SchedulingPipelineTest {

    private SchedulingPipeline pipelineWithStrategy(String strategyName, Map<String, Integer> weights) {
        var configRepo = mock(SchedulingConfigRepository.class);
        var configRecord = new SchedulingConfigRecord(strategyName, weights);
        when(configRepo.findAll()).thenReturn(List.of(configRecord));
        var configService = new SchedulingConfigService(configRepo);

        List<WorkerFilter> filters = List.of(
            new HealthFilter(),
            new ExecutorCapabilityFilter(),
            new ResourceSufficiencyFilter(),
            new ConstraintFilter()
        );
        List<WorkerScorer> scorerList = List.of(
            new ResourceAvailabilityScorer(),
            new QueueDepthScorer()
        );

        return new SchedulingPipeline(filters, scorerList, configService);
    }

    @Test
    void roundRobinRotatesThroughCandidates() {
        var pipeline = pipelineWithStrategy("ROUND_ROBIN", Map.of());
        var w1 = workerRecord("w1");
        var w2 = workerRecord("w2");
        var task = anyTask();

        var first = pipeline.select(task, List.of(w1, w2));
        var second = pipeline.select(task, List.of(w1, w2));
        assertThat(Set.of(first.get().getId(), second.get().getId()))
            .containsExactlyInAnyOrder("w1", "w2");
    }

    @Test
    void resourceFitSelectsBestResourceMatch() {
        var pipeline = pipelineWithStrategy("RESOURCE_FIT",
            Map.of("resourceAvailability", 80, "queueDepth", 20));
        var loaded = workerWithCapacity("loaded", 8, 4096, 1000, 6, 3000, 800);
        var free = workerWithCapacity("free", 8, 4096, 1000, 1, 256, 50);

        var selected = pipeline.select(anyTask(), List.of(loaded, free));
        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo("free");
    }

    @Test
    void filteringRemovesIneligibleBeforeScoring() {
        var pipeline = pipelineWithStrategy("RESOURCE_FIT",
            Map.of("resourceAvailability", 80, "queueDepth", 20));
        var healthy = workerRecord("w1", WorkerHealthState.HEALTHY);
        var dead = workerRecord("w2", WorkerHealthState.DEAD);

        var selected = pipeline.select(anyTask(), List.of(healthy, dead));
        assertThat(selected.get().getId()).isEqualTo("w1");
    }

    @Test
    void emptyAfterFilteringReturnsEmpty() {
        var pipeline = pipelineWithStrategy("RESOURCE_FIT", Map.of());
        var dockerOnly = workerWithExecutors("w1", Set.of(ExecutorType.DOCKER));
        var shellTask = taskWithExecutor(ExecutorType.SHELL);

        var selected = pipeline.select(shellTask, List.of(dockerOnly));
        assertThat(selected).isEmpty();
    }

    @Test
    void noCandidatesReturnsEmpty() {
        var pipeline = pipelineWithStrategy("ROUND_ROBIN", Map.of());
        var selected = pipeline.select(anyTask(), List.of());
        assertThat(selected).isEmpty();
    }

    @Test
    void leastConnectionsSelectsIdleWorker() {
        var pipeline = pipelineWithStrategy("LEAST_CONNECTIONS", Map.of("queueDepth", 100));
        var busy = workerWithCapacity("busy", 8, 4096, 1000, 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            busy.allocateResources(new com.cloudbalancer.common.model.ResourceProfile(0, 0, 0, false, 0, false));
        }
        var idle = workerWithCapacity("idle", 8, 4096, 1000, 0, 0, 0);

        var selected = pipeline.select(anyTask(), List.of(busy, idle));
        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo("idle");
    }
}

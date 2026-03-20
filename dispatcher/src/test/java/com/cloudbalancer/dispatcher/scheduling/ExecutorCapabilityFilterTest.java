package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ExecutorType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class ExecutorCapabilityFilterTest {

    private final ExecutorCapabilityFilter filter = new ExecutorCapabilityFilter();

    @Test
    void removesWorkersWithoutRequiredExecutor() {
        var dockerTask = taskWithExecutor(ExecutorType.DOCKER);
        var dockerWorker = workerWithExecutors("w1", Set.of(ExecutorType.DOCKER, ExecutorType.SHELL));
        var shellOnly = workerWithExecutors("w2", Set.of(ExecutorType.SHELL));

        var result = filter.filter(dockerTask, List.of(dockerWorker, shellOnly));
        assertThat(result).containsExactly(dockerWorker);
    }

    @Test
    void keepsAllCapableWorkers() {
        var simTask = taskWithExecutor(ExecutorType.SIMULATED);
        var w1 = workerWithExecutors("w1", Set.of(ExecutorType.SIMULATED));
        var w2 = workerWithExecutors("w2", Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL));

        var result = filter.filter(simTask, List.of(w1, w2));
        assertThat(result).containsExactly(w1, w2);
    }
}

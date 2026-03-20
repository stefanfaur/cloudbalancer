package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.WorkerHealthState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class HealthFilterTest {

    private final HealthFilter filter = new HealthFilter();

    @Test
    void removesUnhealthyWorkers() {
        var healthy = workerRecord("w1", WorkerHealthState.HEALTHY);
        var suspect = workerRecord("w2", WorkerHealthState.SUSPECT);
        var dead = workerRecord("w3", WorkerHealthState.DEAD);

        var result = filter.filter(anyTask(), List.of(healthy, suspect, dead));
        assertThat(result).containsExactly(healthy);
    }

    @Test
    void keepsAllHealthyWorkers() {
        var w1 = workerRecord("w1", WorkerHealthState.HEALTHY);
        var w2 = workerRecord("w2", WorkerHealthState.HEALTHY);

        var result = filter.filter(anyTask(), List.of(w1, w2));
        assertThat(result).containsExactly(w1, w2);
    }

    @Test
    void returnsEmptyWhenNoneHealthy() {
        var dead = workerRecord("w1", WorkerHealthState.DEAD);
        var result = filter.filter(anyTask(), List.of(dead));
        assertThat(result).isEmpty();
    }
}

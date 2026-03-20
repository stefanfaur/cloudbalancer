package com.cloudbalancer.dispatcher.scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceSufficiencyFilterTest {

    private final ResourceSufficiencyFilter filter = new ResourceSufficiencyFilter();

    @Test
    void removesOverAllocatedWorkers() {
        var task = taskWithResources(4, 2048, 500);
        // bigWorker: 8 total, 2 allocated → 6 free CPU. Enough.
        var bigWorker = workerWithCapacity("w1", 8, 4096, 1000, 2, 1024, 200);
        // smallWorker: 4 total, 2 allocated → 2 free CPU. Not enough for 4.
        var smallWorker = workerWithCapacity("w2", 4, 2048, 500, 2, 1024, 200);

        var result = filter.filter(task, List.of(bigWorker, smallWorker));
        assertThat(result).containsExactly(bigWorker);
    }

    @Test
    void keepsWorkersWithExactResources() {
        var task = taskWithResources(2, 512, 100);
        var worker = workerWithCapacity("w1", 4, 1024, 200, 2, 512, 100);
        // 2 free CPU, 512 free mem, 100 free disk — exactly enough

        var result = filter.filter(task, List.of(worker));
        assertThat(result).containsExactly(worker);
    }

    @Test
    void removesByMemoryInsufficiency() {
        var task = taskWithResources(1, 4096, 100);
        // Has enough CPU and disk, but not memory
        var worker = workerWithCapacity("w1", 8, 2048, 1000, 0, 0, 0);

        var result = filter.filter(task, List.of(worker));
        assertThat(result).isEmpty();
    }
}

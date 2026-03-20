package com.cloudbalancer.dispatcher.scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.cloudbalancer.dispatcher.scheduling.FilterTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConstraintFilterTest {

    private final ConstraintFilter filter = new ConstraintFilter();

    @Test
    void enforcesRequiredTags() {
        var task = taskWithConstraints(Set.of("gpu-enabled"), Set.of(), Set.of());
        var gpuWorker = workerWithTags("w1", Set.of("gpu-enabled", "region-eu"));
        var cpuWorker = workerWithTags("w2", Set.of("region-eu"));

        var result = filter.filter(task, List.of(gpuWorker, cpuWorker));
        assertThat(result).containsExactly(gpuWorker);
    }

    @Test
    void enforcesBlacklist() {
        var task = taskWithConstraints(Set.of(), Set.of("w2"), Set.of());
        var w1 = workerRecord("w1");
        var w2 = workerRecord("w2");

        var result = filter.filter(task, List.of(w1, w2));
        assertThat(result).containsExactly(w1);
    }

    @Test
    void enforcesWhitelist() {
        var task = taskWithConstraints(Set.of(), Set.of(), Set.of("w1"));
        var w1 = workerRecord("w1");
        var w2 = workerRecord("w2");

        var result = filter.filter(task, List.of(w1, w2));
        assertThat(result).containsExactly(w1);
    }

    @Test
    void unconstrainedKeepsAll() {
        var task = anyTask(); // unconstrained
        var w1 = workerRecord("w1");
        var w2 = workerRecord("w2");

        var result = filter.filter(task, List.of(w1, w2));
        assertThat(result).containsExactly(w1, w2);
    }

    @Test
    void combinedConstraints() {
        // Require gpu-enabled tag AND whitelist w1,w3
        var task = taskWithConstraints(Set.of("gpu-enabled"), Set.of(), Set.of("w1", "w3"));
        var w1 = workerWithTags("w1", Set.of("gpu-enabled"));
        var w2 = workerWithTags("w2", Set.of("gpu-enabled")); // not whitelisted
        var w3 = workerWithTags("w3", Set.of()); // no gpu tag

        var result = filter.filter(task, List.of(w1, w2, w3));
        assertThat(result).containsExactly(w1); // only w1 matches both
    }
}

package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class WorkerRegistryServiceTest {

    private final WorkerRegistryService registry = new WorkerRegistryService();

    @Test
    void registerWorkerAddsToRegistry() {
        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED), new ResourceProfile(4, 8192, 10240, false, 0, true), Set.of()
        );
        var worker = new WorkerInfo("worker-1", WorkerHealthState.HEALTHY, capabilities, null, Instant.now());
        registry.registerWorker(worker);

        assertThat(registry.getWorker("worker-1")).isNotNull();
        assertThat(registry.getAvailableWorkers()).hasSize(1);
    }

    @Test
    void nextWorkerRoundRobinCycles() {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED), new ResourceProfile(4, 8192, 10240, false, 0, true), Set.of()
        );
        registry.registerWorker(new WorkerInfo("w-1", WorkerHealthState.HEALTHY, caps, null, Instant.now()));
        registry.registerWorker(new WorkerInfo("w-2", WorkerHealthState.HEALTHY, caps, null, Instant.now()));
        registry.registerWorker(new WorkerInfo("w-3", WorkerHealthState.HEALTHY, caps, null, Instant.now()));

        // Round-robin should cycle through all workers
        Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            WorkerInfo next = registry.nextWorkerRoundRobin();
            assertThat(next).isNotNull();
            seen.add(next.id());
        }
        assertThat(seen).containsExactlyInAnyOrder("w-1", "w-2", "w-3");
    }

    @Test
    void nextWorkerReturnsNullWhenEmpty() {
        assertThat(registry.nextWorkerRoundRobin()).isNull();
    }
}

package com.cloudbalancer.common.runtime;

import com.cloudbalancer.common.model.ExecutorType;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class LocalThreadRuntimeTest {

    @Test
    void startsWithNoWorkers() {
        var runtime = new LocalThreadRuntime();
        assertThat(runtime.listWorkers()).isEmpty();
    }

    @Test
    void startWorkerRegistersIt() {
        var runtime = new LocalThreadRuntime();
        var config = new WorkerConfig(
            "test-worker-1",
            Set.of(ExecutorType.SIMULATED),
            4, 8192, 10240,
            Set.of()
        );
        runtime.startWorker(config);
        assertThat(runtime.listWorkers()).hasSize(1);
        assertThat(runtime.getWorkerInfo("test-worker-1")).isNotNull();
        assertThat(runtime.getWorkerInfo("test-worker-1").id()).isEqualTo("test-worker-1");
    }

    @Test
    void stopWorkerRemovesIt() {
        var runtime = new LocalThreadRuntime();
        var config = new WorkerConfig("test-worker-1", Set.of(ExecutorType.SIMULATED), 4, 8192, 10240, Set.of());
        runtime.startWorker(config);
        assertThat(runtime.listWorkers()).hasSize(1);

        runtime.stopWorker("test-worker-1");
        assertThat(runtime.listWorkers()).isEmpty();
    }

    @Test
    void getWorkerInfoReturnsNullForUnknownWorker() {
        var runtime = new LocalThreadRuntime();
        assertThat(runtime.getWorkerInfo("nonexistent")).isNull();
    }
}

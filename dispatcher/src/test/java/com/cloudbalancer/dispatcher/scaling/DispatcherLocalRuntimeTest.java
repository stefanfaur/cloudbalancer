package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class DispatcherLocalRuntimeTest {

    @Autowired DispatcherLocalRuntime localRuntime;
    @Autowired WorkerRepository workerRepository;

    @BeforeEach
    void cleanUp() {
        // Stop any previously started local workers
        for (var w : localRuntime.listWorkers()) {
            localRuntime.stopWorker(w.id());
        }
        workerRepository.deleteAll();
    }

    @Test
    void startWorkerRegistersInDatabase() {
        var config = new WorkerConfig("auto-local-1", Set.of(ExecutorType.SIMULATED), 4, 8192, 10240, Set.of());
        boolean result = localRuntime.startWorker(config);

        assertThat(result).isTrue();
        assertThat(workerRepository.findById("auto-local-1")).isPresent();
        assertThat(localRuntime.listWorkers()).hasSize(1);
    }

    @Test
    void stopWorkerRemovesFromTracking() {
        var config = new WorkerConfig("auto-local-1", Set.of(ExecutorType.SIMULATED), 4, 8192, 10240, Set.of());
        localRuntime.startWorker(config);

        localRuntime.stopWorker("auto-local-1");

        assertThat(localRuntime.listWorkers()).isEmpty();
    }
}

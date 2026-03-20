package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class WorkerRegistryServiceJpaTest {

    @Autowired WorkerRegistryService workerRegistry;
    @Autowired WorkerRepository workerRepository;
    @Autowired TaskRepository taskRepository;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        workerRepository.deleteAll();
    }

    @Test
    void registerAndRetrieveWorker() {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED), new ResourceProfile(8, 4096, 1000, false, 0, false),
            Set.of("region-eu"));
        workerRegistry.registerWorker("w1", WorkerHealthState.HEALTHY, caps);

        var worker = workerRegistry.getWorker("w1");
        assertThat(worker).isNotNull();
        assertThat(worker.getCapabilities().supportedExecutors()).contains(ExecutorType.SIMULATED);
    }

    @Test
    void allocateAndReleaseResources() {
        registerWorker("w1", 8, 4096, 1000);
        var profile = new ResourceProfile(2, 512, 100, false, 60, false);

        workerRegistry.allocateResources("w1", profile);
        var w = workerRepository.findById("w1").orElseThrow();
        assertThat(w.getAllocatedCpu()).isEqualTo(2);
        assertThat(w.getActiveTaskCount()).isEqualTo(1);

        workerRegistry.releaseResources("w1", profile);
        w = workerRepository.findById("w1").orElseThrow();
        assertThat(w.getAllocatedCpu()).isEqualTo(0);
        assertThat(w.getActiveTaskCount()).isEqualTo(0);
    }

    @Test
    void rebuildLedgerFromPersistedTasks() {
        registerWorker("w1", 8, 4096, 1000);

        // Create 2 tasks in RUNNING state assigned to w1
        var t1 = createAssignedTask("w1", 2, 512, 100);
        var t2 = createAssignedTask("w1", 1, 256, 50);

        workerRegistry.rebuildResourceLedger();

        var w = workerRepository.findById("w1").orElseThrow();
        assertThat(w.getAllocatedCpu()).isEqualTo(3);
        assertThat(w.getAllocatedMemoryMb()).isEqualTo(768);
        assertThat(w.getAllocatedDiskMb()).isEqualTo(150);
        assertThat(w.getActiveTaskCount()).isEqualTo(2);
    }

    @Test
    void getAvailableWorkersReturnsOnlyHealthy() {
        registerWorker("w1", 4, 2048, 500);
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED), new ResourceProfile(4, 2048, 500, false, 0, false), Set.of());
        workerRegistry.registerWorker("w2", WorkerHealthState.DEAD, caps);

        var available = workerRegistry.getAvailableWorkers();
        assertThat(available).hasSize(1);
        assertThat(available.getFirst().getId()).isEqualTo("w1");
    }

    private void registerWorker(String id, int cpu, int mem, int disk) {
        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(cpu, mem, disk, false, 0, false), Set.of());
        workerRegistry.registerWorker(id, WorkerHealthState.HEALTHY, caps);
    }

    private TaskRecord createAssignedTask(String workerId, int cpu, int mem, int disk) {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100),
            new ResourceProfile(cpu, mem, disk, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none());
        var record = TaskRecord.create(descriptor);
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        record.transitionTo(TaskState.ASSIGNED);
        record.setAssignedWorkerId(workerId);
        record.transitionTo(TaskState.PROVISIONING);
        record.transitionTo(TaskState.RUNNING);
        return taskRepository.save(record);
    }
}

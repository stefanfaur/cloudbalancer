package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.cloudbalancer.dispatcher.test.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainersConfig.class)
class TaskAssignmentServiceTest {

    @Autowired TaskAssignmentService assignmentService;
    @Autowired TaskService taskService;
    @Autowired TaskRepository taskRepository;
    @Autowired WorkerRepository workerRepository;
    @Autowired WorkerRegistryService workerRegistry;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        workerRepository.deleteAll();
    }

    @Test
    void assignsTaskToEligibleWorker() {
        registerWorker("w1", Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("w1");
    }

    @Test
    void assignsHighPriorityFirst() {
        // One worker with limited resources (can handle 1 task of 4 cpu)
        registerWorker("w1", Set.of(ExecutorType.SIMULATED), 4, 2048, 500);
        submitTask(Priority.LOW, ExecutorType.SIMULATED, 4, 2048, 500);
        submitTask(Priority.CRITICAL, ExecutorType.SIMULATED, 4, 2048, 500);

        assignmentService.assignPendingTasks();

        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        // CRITICAL should be assigned (it's processed first due to priority ordering)
        assertThat(assigned).anyMatch(t -> t.getPriority() == Priority.CRITICAL);
    }

    @Test
    void updatesResourceLedgerOnAssignment() {
        registerWorker("w1", Set.of(ExecutorType.SIMULATED), 8, 4096, 1000);
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 2, 512, 100);

        assignmentService.assignPendingTasks();

        var worker = workerRepository.findById("w1").orElseThrow();
        assertThat(worker.getAllocatedCpu()).isEqualTo(2);
        assertThat(worker.getAllocatedMemoryMb()).isEqualTo(512);
        assertThat(worker.getAllocatedDiskMb()).isEqualTo(100);
        assertThat(worker.getActiveTaskCount()).isEqualTo(1);
    }

    @Test
    void skipsTaskWhenNoEligibleWorker() {
        // No workers registered
        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);

        assignmentService.assignPendingTasks();

        var queued = taskRepository.findByState(TaskState.QUEUED);
        assertThat(queued).hasSize(1);
    }

    @Test
    void skipsTaskWhenNoCompatibleExecutor() {
        registerWorker("w1", Set.of(ExecutorType.DOCKER), 8, 4096, 1000);
        submitTask(Priority.NORMAL, ExecutorType.SHELL, 1, 256, 50);

        assignmentService.assignPendingTasks();

        var queued = taskRepository.findByState(TaskState.QUEUED);
        assertThat(queued).hasSize(1);
        var assigned = taskRepository.findByState(TaskState.ASSIGNED);
        assertThat(assigned).isEmpty();
    }

    @Test
    void skipsDeadWorkers() {
        var record = new WorkerRecord("dead-w", WorkerHealthState.DEAD,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(8, 4096, 1000, false, 0, false), Set.of()),
            Instant.now());
        workerRepository.save(record);

        submitTask(Priority.NORMAL, ExecutorType.SIMULATED, 1, 256, 50);
        assignmentService.assignPendingTasks();

        var queued = taskRepository.findByState(TaskState.QUEUED);
        assertThat(queued).hasSize(1);
    }

    private void registerWorker(String id, Set<ExecutorType> executors, int cpu, int mem, int disk) {
        var caps = new WorkerCapabilities(executors,
            new ResourceProfile(cpu, mem, disk, false, 0, false), Set.of());
        workerRegistry.registerWorker(id, WorkerHealthState.HEALTHY, caps);
    }

    private void submitTask(Priority priority, ExecutorType executorType, int cpu, int mem, int disk) {
        var descriptor = new TaskDescriptor(
            executorType, Map.of("durationMs", 100),
            new ResourceProfile(cpu, mem, disk, false, 10, false),
            TaskConstraints.unconstrained(), priority,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        taskService.submitTask(descriptor);
    }
}

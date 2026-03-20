package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TaskServiceTest {

    private final TaskService taskService = new TaskService();

    @Test
    void submitTaskCreatesEnvelopeInQueuedState() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );

        TaskEnvelope envelope = taskService.submitTask(descriptor);

        assertThat(envelope.getId()).isNotNull();
        assertThat(envelope.getState()).isEqualTo(TaskState.QUEUED);
        assertThat(envelope.getDescriptor()).isEqualTo(descriptor);
    }

    @Test
    void getTaskByIdReturnsStoredTask() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope created = taskService.submitTask(descriptor);

        TaskEnvelope retrieved = taskService.getTask(created.getId());

        assertThat(retrieved).isSameAs(created);
    }

    @Test
    void getTaskByIdReturnsNullForUnknown() {
        assertThat(taskService.getTask(UUID.randomUUID())).isNull();
    }

    @Test
    void listTasksReturnsAll() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        taskService.submitTask(descriptor);
        taskService.submitTask(descriptor);

        assertThat(taskService.listTasks()).hasSize(2);
    }

    @Test
    void getQueuedTasksReturnsOnlyQueued() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope task1 = taskService.submitTask(descriptor);
        TaskEnvelope task2 = taskService.submitTask(descriptor);
        task1.transitionTo(TaskState.ASSIGNED); // manually move out of QUEUED

        assertThat(taskService.getQueuedTasks()).hasSize(1);
        assertThat(taskService.getQueuedTasks().getFirst().getId()).isEqualTo(task2.getId());
    }
}

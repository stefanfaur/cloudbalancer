package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.TaskAssignedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceTest {

    @Mock private TaskService taskService;
    @Mock private WorkerRegistryService workerRegistry;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private TaskAssignmentService assignmentService;

    private TaskEnvelope createQueuedTask() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        TaskEnvelope envelope = TaskEnvelope.create(descriptor);
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        return envelope;
    }

    @Test
    void assignsQueuedTaskToAvailableWorker() {
        TaskEnvelope task = createQueuedTask();
        when(taskService.getQueuedTasks()).thenReturn(java.util.List.of(task));

        var caps = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED), new ResourceProfile(4, 8192, 10240, false, 0, true), Set.of()
        );
        var worker = new WorkerInfo("w-1", WorkerHealthState.HEALTHY, caps, null, Instant.now());
        when(workerRegistry.nextWorkerRoundRobin()).thenReturn(worker);

        assignmentService.assignPendingTasks();

        assertThat(task.getState()).isEqualTo(TaskState.ASSIGNED);
        verify(eventPublisher).publishMessage(eq("tasks.assigned"), eq("w-1"), any(TaskAssignment.class));
        verify(eventPublisher).publishEvent(eq("tasks.events"), eq(task.getId().toString()), any(TaskAssignedEvent.class));
    }

    @Test
    void noAssignmentWhenNoWorkersAvailable() {
        TaskEnvelope task = createQueuedTask();
        when(taskService.getQueuedTasks()).thenReturn(java.util.List.of(task));
        when(workerRegistry.nextWorkerRoundRobin()).thenReturn(null);

        assignmentService.assignPendingTasks();

        assertThat(task.getState()).isEqualTo(TaskState.QUEUED); // stays queued
        verify(eventPublisher, never()).publishMessage(anyString(), anyString(), any());
    }

    @Test
    void noAssignmentWhenNoQueuedTasks() {
        when(taskService.getQueuedTasks()).thenReturn(java.util.List.of());

        assignmentService.assignPendingTasks();

        verify(workerRegistry, never()).nextWorkerRoundRobin();
    }
}

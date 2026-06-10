package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskStatusListenerTest {

    @Mock
    private TaskService taskService;

    private String statusJson(UUID taskId, UUID executionId, TaskState state) throws Exception {
        return JsonUtil.mapper().writeValueAsString(
            new TaskStatusUpdate(taskId, "worker-1", executionId, state, Instant.now()));
    }

    @Test
    void transitionsAssignedTaskToRunningOnMatchingExecution() throws Exception {
        UUID exec = UUID.randomUUID();
        TaskRecord task = assignedTask(exec);
        when(taskService.getTaskRecord(task.getId())).thenReturn(task);
        var listener = new TaskStatusListener(taskService);

        listener.onTaskStatus(statusJson(task.getId(), exec, TaskState.RUNNING));

        assertThat(task.getState()).isEqualTo(TaskState.RUNNING);
        assertThat(task.getStartedAt()).isNotNull();
        verify(taskService).updateTask(task);
    }

    @Test
    void ignoresStatusWithStaleExecutionId() throws Exception {
        TaskRecord task = assignedTask(UUID.randomUUID());
        when(taskService.getTaskRecord(task.getId())).thenReturn(task);
        var listener = new TaskStatusListener(taskService);

        listener.onTaskStatus(statusJson(task.getId(), UUID.randomUUID(), TaskState.RUNNING));

        assertThat(task.getState()).isEqualTo(TaskState.ASSIGNED);
        verify(taskService, never()).updateTask(any());
    }

    @Test
    void ignoresStatusForTerminalTask() throws Exception {
        UUID exec = UUID.randomUUID();
        TaskRecord task = assignedTask(exec);
        task.transitionTo(TaskState.CANCELLED); // terminal
        when(taskService.getTaskRecord(task.getId())).thenReturn(task);
        var listener = new TaskStatusListener(taskService);

        listener.onTaskStatus(statusJson(task.getId(), exec, TaskState.RUNNING));

        verify(taskService, never()).updateTask(any());
    }

    @Test
    void ignoresStatusForUnknownTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(taskService.getTaskRecord(taskId)).thenReturn(null);
        var listener = new TaskStatusListener(taskService);

        listener.onTaskStatus(statusJson(taskId, UUID.randomUUID(), TaskState.RUNNING));

        verify(taskService, never()).updateTask(any());
    }

    private TaskRecord assignedTask(UUID executionId) {
        TaskDescriptor descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 100),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            new ExecutionPolicy(3, 300, BackoffStrategy.FIXED, FailureAction.RETRY),
            TaskIO.none()
        );
        TaskRecord task = TaskRecord.create(descriptor);
        task.transitionTo(TaskState.VALIDATED);
        task.transitionTo(TaskState.QUEUED);
        task.transitionTo(TaskState.ASSIGNED);
        task.setAssignedWorkerId("worker-1");
        task.setCurrentExecutionId(executionId);
        return task;
    }
}

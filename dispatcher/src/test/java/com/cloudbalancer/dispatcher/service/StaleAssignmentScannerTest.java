package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaleAssignmentScannerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkerFailureHandler workerFailureHandler;

    private StaleAssignmentScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new StaleAssignmentScanner(taskRepository, workerFailureHandler, 60);
    }

    @Test
    void assignedTaskOlderThanThresholdIsRequeued() {
        TaskRecord task = createAssignedTask(Instant.now().minusSeconds(120));
        when(taskRepository.findByStateIn(List.of(TaskState.ASSIGNED))).thenReturn(List.of(task));

        scanner.scanForStaleAssignments();

        verify(workerFailureHandler).requeueInFlight(eq(task), contains("not picked up"));
    }

    @Test
    void assignedTaskWithinThresholdIsLeftAlone() {
        TaskRecord task = createAssignedTask(Instant.now().minusSeconds(5));
        when(taskRepository.findByStateIn(List.of(TaskState.ASSIGNED))).thenReturn(List.of(task));

        scanner.scanForStaleAssignments();

        verifyNoInteractions(workerFailureHandler);
    }

    @Test
    void assignedTaskWithoutTimestampIsRequeuedDefensively() {
        TaskRecord task = createAssignedTask(null);
        when(taskRepository.findByStateIn(List.of(TaskState.ASSIGNED))).thenReturn(List.of(task));

        scanner.scanForStaleAssignments();

        verify(workerFailureHandler).requeueInFlight(eq(task), anyString());
    }

    @Test
    void noAssignedTasksDoesNothing() {
        when(taskRepository.findByStateIn(List.of(TaskState.ASSIGNED))).thenReturn(List.of());

        scanner.scanForStaleAssignments();

        verifyNoInteractions(workerFailureHandler);
    }

    private TaskRecord createAssignedTask(Instant assignedAt) {
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
        task.setAssignedAt(assignedAt);
        return task;
    }
}

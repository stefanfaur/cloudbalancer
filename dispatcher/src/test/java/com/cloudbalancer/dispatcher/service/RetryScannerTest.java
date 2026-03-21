package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryScannerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private EventPublisher eventPublisher;

    private RetryScanner retryScanner;

    @BeforeEach
    void setUp() {
        retryScanner = new RetryScanner(taskRepository, taskService, eventPublisher, 5, 2000);
    }

    @Test
    void taskWithRetriesEligibleIsRequeued() {
        // Task in FAILED state with 1 attempt, maxRetries=3, retryEligibleAt in the past
        TaskRecord task = createFailedTask(
                new ExecutionPolicy(3, 300, BackoffStrategy.FIXED, FailureAction.RETRY));

        // Add one non-worker-caused attempt
        task.addAttempt(new ExecutionAttempt(
                1, "worker-1", Instant.now().minusSeconds(60), Instant.now().minusSeconds(30),
                1, null, "task error", false, UUID.randomUUID()));
        task.setRetryEligibleAt(Instant.now().minusSeconds(10)); // eligible in the past

        UUID originalExecutionId = task.getCurrentExecutionId();

        when(taskRepository.findByStateIn(anyList())).thenReturn(List.of(task));

        retryScanner.scanAndRetry();

        verify(taskService).updateTask(argThat(t -> {
            assertThat(t.getState()).isEqualTo(TaskState.QUEUED);
            assertThat(t.getAssignedWorkerId()).isNull();
            assertThat(t.getCurrentExecutionId()).isNotEqualTo(originalExecutionId);
            assertThat(t.getRetryEligibleAt()).isNull();
            return true;
        }));
    }

    @Test
    void taskWithMaxRetriesIsDeadLettered() {
        // Task in FAILED state with attempts >= maxRetries
        ExecutionPolicy policy = new ExecutionPolicy(2, 300, BackoffStrategy.FIXED, FailureAction.RETRY);
        TaskRecord task = createFailedTask(policy);

        // Add 2 non-worker-caused attempts (equals maxRetries)
        task.addAttempt(new ExecutionAttempt(
                1, "worker-1", Instant.now().minusSeconds(120), Instant.now().minusSeconds(100),
                1, null, "error", false, UUID.randomUUID()));
        task.addAttempt(new ExecutionAttempt(
                2, "worker-2", Instant.now().minusSeconds(60), Instant.now().minusSeconds(30),
                1, null, "error", false, UUID.randomUUID()));
        task.setRetryEligibleAt(Instant.now().minusSeconds(10)); // eligible

        when(taskRepository.findByStateIn(anyList())).thenReturn(List.of(task));

        retryScanner.scanAndRetry();

        verify(taskService).updateTask(argThat(t -> {
            assertThat(t.getState()).isEqualTo(TaskState.DEAD_LETTERED);
            return true;
        }));
        verify(eventPublisher).publishEvent(eq("tasks.deadletter"), eq(task.getId().toString()), any());
    }

    @Test
    void taskNotYetEligibleIsSkipped() {
        // Task with retryEligibleAt in the future
        TaskRecord task = createFailedTask(
                new ExecutionPolicy(3, 300, BackoffStrategy.FIXED, FailureAction.RETRY));

        task.addAttempt(new ExecutionAttempt(
                1, "worker-1", Instant.now().minusSeconds(60), Instant.now().minusSeconds(30),
                1, null, "error", false, UUID.randomUUID()));
        task.setRetryEligibleAt(Instant.now().plusSeconds(300)); // not yet eligible

        when(taskRepository.findByStateIn(anyList())).thenReturn(List.of(task));

        retryScanner.scanAndRetry();

        verify(taskService, never()).updateTask(any());
        verify(eventPublisher, never()).publishEvent(anyString(), anyString(), any());
    }

    @Test
    void taskWithDeadLetterPolicyIsDeadLetteredImmediately() {
        // Task with failureAction=DEAD_LETTER — should be dead-lettered regardless of attempt count
        TaskRecord task = createFailedTask(
                new ExecutionPolicy(3, 300, BackoffStrategy.FIXED, FailureAction.DEAD_LETTER));

        // Only 1 attempt (well below maxRetries=3)
        task.addAttempt(new ExecutionAttempt(
                1, "worker-1", Instant.now().minusSeconds(60), Instant.now().minusSeconds(30),
                1, null, "error", false, UUID.randomUUID()));
        task.setRetryEligibleAt(Instant.now().minusSeconds(10)); // eligible

        when(taskRepository.findByStateIn(anyList())).thenReturn(List.of(task));

        retryScanner.scanAndRetry();

        verify(taskService).updateTask(argThat(t -> {
            assertThat(t.getState()).isEqualTo(TaskState.DEAD_LETTERED);
            return true;
        }));
        verify(eventPublisher).publishEvent(eq("tasks.deadletter"), eq(task.getId().toString()), any());
    }

    @Test
    void poisonPillDetected() {
        // 3+ fast failures (<2000ms) on 3 distinct workers → dead-lettered
        ExecutionPolicy policy = new ExecutionPolicy(10, 300, BackoffStrategy.FIXED, FailureAction.RETRY);
        TaskRecord task = createFailedTask(policy);

        Instant now = Instant.now();
        // 3 fast failures on 3 distinct workers (each under 2000ms duration)
        task.addAttempt(new ExecutionAttempt(
                1, "worker-A", now.minusSeconds(30), now.minusSeconds(29),
                1, null, "crash", false, UUID.randomUUID()));
        task.addAttempt(new ExecutionAttempt(
                2, "worker-B", now.minusSeconds(20), now.minusSeconds(19),
                1, null, "crash", false, UUID.randomUUID()));
        task.addAttempt(new ExecutionAttempt(
                3, "worker-C", now.minusSeconds(10), now.minusSeconds(9),
                1, null, "crash", false, UUID.randomUUID()));
        task.setRetryEligibleAt(now.minusSeconds(1)); // eligible

        when(taskRepository.findByStateIn(anyList())).thenReturn(List.of(task));

        retryScanner.scanAndRetry();

        verify(taskService).updateTask(argThat(t -> {
            assertThat(t.getState()).isEqualTo(TaskState.DEAD_LETTERED);
            return true;
        }));
        verify(eventPublisher).publishEvent(eq("tasks.deadletter"), eq(task.getId().toString()), argThat(event -> {
            // The event reason should mention "Poison pill"
            return event.toString().contains("Poison pill") ||
                   event.toString().contains("poison pill") ||
                   event.toString().contains("rapid failures");
        }));
    }

    /**
     * Helper: create a TaskRecord and transition it to FAILED through valid state transitions.
     * Path: SUBMITTED → VALIDATED → QUEUED → ASSIGNED → PROVISIONING → RUNNING → FAILED
     */
    private TaskRecord createFailedTask(ExecutionPolicy executionPolicy) {
        TaskDescriptor descriptor = new TaskDescriptor(
                ExecutorType.SIMULATED,
                Map.of("durationMs", 100, "failProbability", 1.0),
                new ResourceProfile(1, 512, 256, false, 10, false),
                TaskConstraints.unconstrained(),
                Priority.NORMAL,
                executionPolicy,
                TaskIO.none()
        );

        TaskRecord task = TaskRecord.create(descriptor);
        task.transitionTo(TaskState.VALIDATED);
        task.transitionTo(TaskState.QUEUED);
        task.transitionTo(TaskState.ASSIGNED);
        task.transitionTo(TaskState.PROVISIONING);
        task.transitionTo(TaskState.RUNNING);
        task.transitionTo(TaskState.FAILED);
        return task;
    }
}

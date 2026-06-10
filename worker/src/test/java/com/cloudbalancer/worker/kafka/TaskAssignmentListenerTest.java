package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.service.TaskExecutionService;
import com.cloudbalancer.worker.service.WorkerRegistrationService;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentListenerTest {

    @Mock
    private TaskExecutionService executionService;

    @Mock
    private WorkerRegistrationService registrationService;

    @Mock
    private ConsumerSeekAware.ConsumerSeekCallback seekCallback;

    private TaskAssignmentListener listener;

    @BeforeEach
    void setUp() {
        // Direct executor: run submitted tasks inline so behavior is observable
        // synchronously in tests.
        listener = new TaskAssignmentListener(
            executionService, registrationService, new AtomicBoolean(false), "worker-1",
            Runnable::run);
    }

    @Test
    void registersWorkerOnceAssignmentPartitionsAreAssigned() {
        // The consumer's position is fixed at partition assignment; only then
        // is it safe to announce readiness — assignments published earlier
        // would be below the 'latest' reset position and silently lost.
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 0L), seekCallback);

        verify(registrationService).registerOnce();
    }

    @Test
    void everyRebalanceDelegatesToIdempotentRegistration() {
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 0L), seekCallback);
        listener.onPartitionsAssigned(
            Map.of(new TopicPartition("tasks.assigned", 0), 5L), seekCallback);

        // delegate every time; WorkerRegistrationService.registerOnce dedupes
        verify(registrationService, times(2)).registerOnce();
    }

    @Test
    void submitsAcceptedTaskToIntakeExecutor() throws Exception {
        // The listener must hand execution to the bounded intake executor rather
        // than running it inline on the Kafka consumer thread.
        Executor intake = mock(Executor.class);
        var l = new TaskAssignmentListener(
            executionService, registrationService, new AtomicBoolean(false), "worker-1", intake);

        l.onTaskAssigned(assignmentJson("worker-1"));

        verify(intake).execute(any(Runnable.class));
    }

    @Test
    void intakeExecutorRunsTheTaskExecution() throws Exception {
        // With a direct executor the submitted task runs, invoking execution.
        listener.onTaskAssigned(assignmentJson("worker-1"));

        verify(executionService).executeTask(any(TaskAssignment.class));
    }

    @Test
    void drainingWorkerDoesNotSubmit() throws Exception {
        Executor intake = mock(Executor.class);
        var l = new TaskAssignmentListener(
            executionService, registrationService, new AtomicBoolean(true), "worker-1", intake);

        l.onTaskAssigned(assignmentJson("worker-1"));

        verify(intake, never()).execute(any());
        verify(executionService, never()).executeTask(any());
    }

    @Test
    void ignoresAssignmentForAnotherWorker() throws Exception {
        Executor intake = mock(Executor.class);
        var l = new TaskAssignmentListener(
            executionService, registrationService, new AtomicBoolean(false), "worker-1", intake);

        l.onTaskAssigned(assignmentJson("some-other-worker"));

        verify(intake, never()).execute(any());
        verify(executionService, never()).executeTask(any());
    }

    private String assignmentJson(String assignedWorkerId) throws Exception {
        TaskDescriptor descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 100),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            new ExecutionPolicy(3, 300, BackoffStrategy.FIXED, FailureAction.RETRY),
            TaskIO.none()
        );
        var assignment = new TaskAssignment(
            UUID.randomUUID(), descriptor, assignedWorkerId, Instant.now(), UUID.randomUUID());
        return JsonUtil.mapper().writeValueAsString(assignment);
    }
}

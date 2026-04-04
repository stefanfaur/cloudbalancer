package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.event.TaskStateChangedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.service.AutoScalerService;
import com.cloudbalancer.dispatcher.service.TaskService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.cloudbalancer.dispatcher.util.BackoffCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class TaskResultListener {

    private static final Logger log = LoggerFactory.getLogger(TaskResultListener.class);
    private final TaskService taskService;
    private final WorkerRegistryService workerRegistryService;
    private final EventPublisher eventPublisher;
    private final AutoScalerService autoScalerService;
    private final long baseDelaySeconds;

    public TaskResultListener(TaskService taskService, WorkerRegistryService workerRegistryService,
                              EventPublisher eventPublisher, AutoScalerService autoScalerService,
                              @Value("${cloudbalancer.retry.base-delay-seconds:5}") long baseDelaySeconds) {
        this.taskService = taskService;
        this.workerRegistryService = workerRegistryService;
        this.eventPublisher = eventPublisher;
        this.autoScalerService = autoScalerService;
        this.baseDelaySeconds = baseDelaySeconds;
    }

    @KafkaListener(topics = "tasks.results", groupId = "dispatcher")
    public void onTaskResult(String message) {
        try {
            TaskResult result = JsonUtil.mapper().readValue(message, TaskResult.class);
            TaskRecord record = taskService.getTaskRecord(result.taskId());
            if (record == null) {
                log.warn("Received result for unknown task: {}", result.taskId());
                return;
            }

            // Check idempotency — discard stale results from previous assignments
            if (result.executionId() != null && record.getCurrentExecutionId() != null
                    && !result.executionId().equals(record.getCurrentExecutionId())) {
                log.warn("Stale result for task {}: executionId mismatch (result={}, current={}), discarding",
                    result.taskId(), result.executionId(), record.getCurrentExecutionId());
                return;
            }

            // Guard against duplicate Kafka delivery — task already reached a terminal state
            if (record.getState().isTerminal()) {
                log.info("Ignoring duplicate result for task {} (already {})", result.taskId(), record.getState());
                return;
            }

            TaskState previousState = record.getState();

            // Fast-forward through intermediate states
            if (record.getState() == TaskState.ASSIGNED) {
                record.transitionTo(TaskState.PROVISIONING);
            }
            if (record.getState() == TaskState.PROVISIONING) {
                record.transitionTo(TaskState.RUNNING);
            }

            // Set lifecycle timestamps
            if (record.getStartedAt() == null) {
                record.setStartedAt(result.completedAt().minusMillis(result.executionDurationMs()));
            }
            record.setCompletedAt(result.completedAt());

            if (result.timedOut()) {
                record.transitionTo(TaskState.TIMED_OUT);
            } else if (result.succeeded()) {
                record.transitionTo(TaskState.POST_PROCESSING);
                record.transitionTo(TaskState.COMPLETED);
            } else {
                record.transitionTo(TaskState.FAILED);
            }

            // Calculate backoff for retriable failures
            if (record.getState() == TaskState.FAILED || record.getState() == TaskState.TIMED_OUT) {
                int attemptCount = record.getExecutionHistory().size() + 1;
                Instant nextRetryTime = BackoffCalculator.calculateNextRetryTime(
                    record.getDescriptor().executionPolicy().retryBackoffStrategy(),
                    attemptCount,
                    baseDelaySeconds,
                    Instant.now()
                );
                record.setRetryEligibleAt(nextRetryTime);
            }

            // Record execution attempt
            record.addAttempt(new ExecutionAttempt(
                record.getExecutionHistory().size() + 1,
                result.workerId(),
                record.getSubmittedAt(),
                Instant.now(),
                result.exitCode(),
                null,
                result.succeeded() ? null : result.stderr(),
                false,
                result.executionId()
            ));

            // Persist stdout/stderr from execution result
            record.setLastStdout(result.stdout());
            record.setLastStderr(result.stderr());

            taskService.updateTask(record);

            // Notify auto-scaler of task completion for queue-pressure tracking
            if (record.getState().isTerminal()) {
                autoScalerService.recordTaskCompleted();
            }

            // Release resource ledger on terminal state
            if (record.getState().isTerminal() && record.getAssignedWorkerId() != null) {
                workerRegistryService.releaseResources(
                    record.getAssignedWorkerId(),
                    record.getDescriptor().resourceProfile());
            }

            // Publish completion/failure events
            if (result.succeeded()) {
                eventPublisher.publishEvent("tasks.events", result.taskId().toString(),
                    new TaskCompletedEvent(
                        UUID.randomUUID().toString(), Instant.now(),
                        result.taskId(), result.exitCode(), result.stdout(), result.stderr()
                    )
                );
            } else {
                eventPublisher.publishEvent("tasks.events", result.taskId().toString(),
                    new TaskStateChangedEvent(
                        UUID.randomUUID().toString(), Instant.now(),
                        result.taskId(), previousState, record.getState(),
                        result.timedOut() ? "Execution timed out" : "Execution failed: " + result.stderr()
                    )
                );
            }

            log.info("Task {} -> {}", result.taskId(), record.getState());
        } catch (Exception e) {
            log.error("Failed to process task result", e);
        }
    }
}

package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.event.TaskStateChangedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.service.TaskService;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public TaskResultListener(TaskService taskService, WorkerRegistryService workerRegistryService,
                              EventPublisher eventPublisher) {
        this.taskService = taskService;
        this.workerRegistryService = workerRegistryService;
        this.eventPublisher = eventPublisher;
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

            TaskState previousState = record.getState();

            // Fast-forward through intermediate states
            if (record.getState() == TaskState.ASSIGNED) {
                record.transitionTo(TaskState.PROVISIONING);
            }
            if (record.getState() == TaskState.PROVISIONING) {
                record.transitionTo(TaskState.RUNNING);
            }

            if (result.timedOut()) {
                record.transitionTo(TaskState.TIMED_OUT);
            } else if (result.succeeded()) {
                record.transitionTo(TaskState.POST_PROCESSING);
                record.transitionTo(TaskState.COMPLETED);
            } else {
                record.transitionTo(TaskState.FAILED);
            }

            // Record execution attempt
            record.addAttempt(new ExecutionAttempt(
                record.getExecutionHistory().size() + 1,
                result.workerId(),
                record.getSubmittedAt(),
                Instant.now(),
                result.exitCode(),
                null
            ));

            taskService.updateTask(record);

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

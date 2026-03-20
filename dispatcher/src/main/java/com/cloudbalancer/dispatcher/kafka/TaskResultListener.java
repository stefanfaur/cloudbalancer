package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.event.TaskCompletedEvent;
import com.cloudbalancer.common.event.TaskStateChangedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.service.TaskService;
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
    private final EventPublisher eventPublisher;

    public TaskResultListener(TaskService taskService, EventPublisher eventPublisher) {
        this.taskService = taskService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "tasks.results", groupId = "dispatcher")
    public void onTaskResult(String message) {
        try {
            TaskResult result = JsonUtil.mapper().readValue(message, TaskResult.class);
            TaskEnvelope envelope = taskService.getTask(result.taskId());
            if (envelope == null) {
                log.warn("Received result for unknown task: {}", result.taskId());
                return;
            }

            TaskState previousState = envelope.getState();

            // Fast-forward through intermediate states that the worker didn't report
            if (envelope.getState() == TaskState.ASSIGNED) {
                envelope.transitionTo(TaskState.PROVISIONING);
            }
            if (envelope.getState() == TaskState.PROVISIONING) {
                envelope.transitionTo(TaskState.RUNNING);
            }

            if (result.timedOut()) {
                envelope.transitionTo(TaskState.TIMED_OUT);
            } else if (result.succeeded()) {
                envelope.transitionTo(TaskState.POST_PROCESSING);
                envelope.transitionTo(TaskState.COMPLETED);
            } else {
                envelope.transitionTo(TaskState.FAILED);
            }

            // Record execution attempt
            envelope.addAttempt(new ExecutionAttempt(
                envelope.getExecutionHistory().size() + 1,
                result.workerId(),
                envelope.getSubmittedAt(), // approximate
                Instant.now(),
                result.exitCode(),
                null
            ));

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
                        result.taskId(), previousState, envelope.getState(),
                        result.timedOut() ? "Execution timed out" : "Execution failed: " + result.stderr()
                    )
                );
            }

            log.info("Task {} -> {}", result.taskId(), envelope.getState());
        } catch (Exception e) {
            log.error("Failed to process task result", e);
        }
    }
}

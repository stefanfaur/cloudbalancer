package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.model.TaskStatusUpdate;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes in-progress {@link TaskStatusUpdate}s published by workers as they
 * begin executing a task, and moves the task out of ASSIGNED accordingly. This
 * makes ASSIGNED a short-lived state again: once a worker reports RUNNING, the
 * stale-assignment scanner no longer considers the task a candidate for
 * re-queuing, preventing duplicate executions of long-running tasks.
 */
@Component
public class TaskStatusListener {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusListener.class);

    private final TaskService taskService;

    public TaskStatusListener(TaskService taskService) {
        this.taskService = taskService;
    }

    @KafkaListener(topics = "tasks.status", groupId = "dispatcher")
    public void onTaskStatus(String message) {
        try {
            TaskStatusUpdate update = JsonUtil.mapper().readValue(message, TaskStatusUpdate.class);
            TaskRecord record = taskService.getTaskRecord(update.taskId());
            if (record == null) {
                log.warn("Received status for unknown task: {}", update.taskId());
                return;
            }

            // Discard status from a superseded execution attempt (the task was
            // re-queued and re-dispatched with a fresh executionId).
            if (update.executionId() != null && record.getCurrentExecutionId() != null
                    && !update.executionId().equals(record.getCurrentExecutionId())) {
                log.debug("Stale status for task {}: executionId mismatch (status={}, current={}), discarding",
                    update.taskId(), update.executionId(), record.getCurrentExecutionId());
                return;
            }

            // The terminal result may have already arrived (results and status
            // race); never resurrect a settled task.
            if (record.getState().isTerminal()) {
                return;
            }

            if (update.state() == TaskState.RUNNING) {
                boolean changed = false;
                if (record.getState() == TaskState.ASSIGNED) {
                    record.transitionTo(TaskState.PROVISIONING);
                    changed = true;
                }
                if (record.getState() == TaskState.PROVISIONING) {
                    record.transitionTo(TaskState.RUNNING);
                    changed = true;
                }
                if (changed) {
                    if (record.getStartedAt() == null) {
                        record.setStartedAt(update.timestamp() != null ? update.timestamp() : Instant.now());
                    }
                    taskService.updateTask(record);
                    log.debug("Task {} moved to RUNNING on worker {}", update.taskId(), update.workerId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process task status update", e);
        }
    }
}

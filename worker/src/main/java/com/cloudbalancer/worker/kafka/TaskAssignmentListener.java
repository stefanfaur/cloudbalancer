package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.TaskAssignment;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskAssignmentListener {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentListener.class);
    private final TaskExecutionService executionService;
    private final AtomicBoolean draining;

    public TaskAssignmentListener(TaskExecutionService executionService, AtomicBoolean draining) {
        this.executionService = executionService;
        this.draining = draining;
    }

    @KafkaListener(topics = "tasks.assigned", groupId = "${cloudbalancer.worker.id:worker-1}")
    public void onTaskAssigned(String message) {
        if (draining.get()) {
            log.info("Worker is draining — ignoring task assignment");
            return;
        }
        try {
            TaskAssignment assignment = JsonUtil.mapper().readValue(message, TaskAssignment.class);
            log.info("Received task assignment: {}", assignment.taskId());
            executionService.executeTask(assignment);
        } catch (Exception e) {
            log.error("Failed to process task assignment", e);
        }
    }
}

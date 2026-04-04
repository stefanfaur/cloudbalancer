package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.TaskAssignment;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskAssignmentListener {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentListener.class);
    private final TaskExecutionService executionService;
    private final AtomicBoolean draining;
    private final String workerId;

    public TaskAssignmentListener(TaskExecutionService executionService, AtomicBoolean draining,
                                  @Value("${cloudbalancer.worker.id:worker-1}") String workerId) {
        this.executionService = executionService;
        this.draining = draining;
        this.workerId = workerId;
    }

    @KafkaListener(topics = "tasks.assigned", groupId = "${cloudbalancer.worker.id:worker-1}")
    public void onTaskAssigned(String message) {
        if (draining.get()) {
            log.info("Worker is draining — ignoring task assignment");
            return;
        }
        try {
            TaskAssignment assignment = JsonUtil.mapper().readValue(message, TaskAssignment.class);

            // Ignore assignments meant for a different worker
            if (!workerId.equals(assignment.assignedWorkerId())) {
                log.debug("Ignoring assignment for worker {} (I am {})", assignment.assignedWorkerId(), workerId);
                return;
            }

            log.info("Received task assignment: {}", assignment.taskId());
            executionService.executeTask(assignment);
        } catch (Exception e) {
            log.error("Failed to process task assignment", e);
        }
    }
}

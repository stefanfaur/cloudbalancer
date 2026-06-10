package com.cloudbalancer.worker.kafka;

import com.cloudbalancer.common.model.TaskAssignment;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.worker.service.TaskExecutionService;
import com.cloudbalancer.worker.service.WorkerRegistrationService;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskAssignmentListener implements ConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentListener.class);
    private final TaskExecutionService executionService;
    private final WorkerRegistrationService registrationService;
    private final AtomicBoolean draining;
    private final String workerId;
    private final Executor taskIntakeExecutor;

    public TaskAssignmentListener(TaskExecutionService executionService,
                                  WorkerRegistrationService registrationService,
                                  AtomicBoolean draining,
                                  @Value("${cloudbalancer.worker.id:worker-1}") String workerId,
                                  Executor taskIntakeExecutor) {
        this.executionService = executionService;
        this.registrationService = registrationService;
        this.draining = draining;
        this.workerId = workerId;
        this.taskIntakeExecutor = taskIntakeExecutor;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments,
                                     ConsumerSeekCallback callback) {
        // The consumer's position is fixed only once partitions are assigned.
        // Register with the dispatcher now — any earlier and assignments
        // could be published below the 'latest' reset position and lost.
        log.info("tasks.assigned partitions assigned: {} — announcing worker readiness",
            assignments.keySet());
        registrationService.registerOnce();
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
            // Run on the bounded intake executor so the Kafka consumer thread is
            // free to keep draining assignments (and report RUNNING promptly).
            // CallerRunsPolicy on a saturated pool applies backpressure here.
            taskIntakeExecutor.execute(() -> executionService.executeTask(assignment));
        } catch (Exception e) {
            log.error("Failed to process task assignment", e);
        }
    }
}

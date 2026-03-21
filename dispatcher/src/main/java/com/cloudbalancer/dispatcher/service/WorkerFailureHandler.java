package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.TaskStateChangedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkerFailureHandler.class);
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final WorkerRegistryService workerRegistry;
    private final EventPublisher eventPublisher;

    public WorkerFailureHandler(TaskRepository taskRepository, TaskService taskService,
                                WorkerRegistryService workerRegistry, EventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
    }

    public void onWorkerDead(String workerId) {
        log.info("Handling death of worker {}: re-queuing in-flight tasks", workerId);

        var inFlightTasks = taskRepository.findByAssignedWorkerIdAndStateIn(
            workerId,
            List.of(TaskState.ASSIGNED, TaskState.PROVISIONING, TaskState.RUNNING)
        );

        for (TaskRecord task : inFlightTasks) {
            TaskState previousState = task.getState();
            log.info("Re-queuing task {} due to worker {} death", task.getId(), workerId);

            // Record failed attempt with worker-caused flag
            var failedAttempt = new ExecutionAttempt(
                task.getExecutionHistory().size() + 1,
                workerId,
                task.getAssignedAt() != null ? task.getAssignedAt() : Instant.now(),
                Instant.now(),
                1,
                null,
                "Worker died",
                true,  // Worker-caused failure — doesn't count against retry limit
                task.getCurrentExecutionId()
            );
            task.addAttempt(failedAttempt);

            // Fast-forward to FAILED first (need valid transition path)
            // From ASSIGNED/PROVISIONING/RUNNING we need to get to FAILED then QUEUED
            if (task.getState() == TaskState.ASSIGNED) {
                task.transitionTo(TaskState.PROVISIONING);
            }
            if (task.getState() == TaskState.PROVISIONING) {
                task.transitionTo(TaskState.RUNNING);
            }
            if (task.getState() == TaskState.RUNNING) {
                task.transitionTo(TaskState.FAILED);
            }
            task.transitionTo(TaskState.QUEUED);

            task.setAssignedWorkerId(null);
            task.setCurrentExecutionId(UUID.randomUUID());
            task.setRetryEligibleAt(Instant.now());  // Immediate eligibility
            taskService.updateTask(task);

            // Release resource ledger
            workerRegistry.releaseResources(workerId, task.getDescriptor().resourceProfile());

            // Publish state change event
            eventPublisher.publishEvent("tasks.events", task.getId().toString(),
                new TaskStateChangedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    task.getId(),
                    previousState,
                    TaskState.QUEUED,
                    "Worker died; re-queued"
                )
            );
        }

        log.info("Re-queued {} tasks due to worker {} death", inFlightTasks.size(), workerId);
    }
}

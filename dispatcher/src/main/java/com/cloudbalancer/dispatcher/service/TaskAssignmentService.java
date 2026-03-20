package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.TaskAssignedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.scheduling.SchedulingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class TaskAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TaskAssignmentService.class);
    private final TaskService taskService;
    private final WorkerRegistryService workerRegistry;
    private final SchedulingPipeline schedulingPipeline;
    private final EventPublisher eventPublisher;

    public TaskAssignmentService(TaskService taskService, WorkerRegistryService workerRegistry,
                                  SchedulingPipeline schedulingPipeline, EventPublisher eventPublisher) {
        this.taskService = taskService;
        this.workerRegistry = workerRegistry;
        this.schedulingPipeline = schedulingPipeline;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.assignment-interval-ms:1000}")
    public void assignPendingTasks() {
        var allWorkers = workerRegistry.getAllWorkers();
        if (allWorkers.isEmpty()) {
            return;
        }

        for (TaskRecord task : taskService.getQueuedTasks()) {
            var selected = schedulingPipeline.select(task, allWorkers);
            if (selected.isEmpty()) {
                log.debug("No eligible worker for task {}, skipping", task.getId());
                continue;
            }

            var worker = selected.get();
            task.transitionTo(TaskState.ASSIGNED);
            task.setAssignedWorkerId(worker.getId());
            task.setAssignedAt(Instant.now());
            taskService.updateTask(task);

            workerRegistry.allocateResources(worker.getId(),
                task.getDescriptor().resourceProfile());

            var assignment = new TaskAssignment(
                task.getId(), task.getDescriptor(), worker.getId(), Instant.now()
            );
            eventPublisher.publishMessage("tasks.assigned", worker.getId(), assignment);

            eventPublisher.publishEvent("tasks.events", task.getId().toString(),
                new TaskAssignedEvent(
                    UUID.randomUUID().toString(), Instant.now(), task.getId(), worker.getId()
                )
            );

            log.info("Assigned task {} to worker {} via pipeline", task.getId(), worker.getId());

            // Refresh worker list to pick up updated resource ledger
            allWorkers = workerRegistry.getAllWorkers();
        }
    }
}

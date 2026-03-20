package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.TaskAssignedEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
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
    private final EventPublisher eventPublisher;

    public TaskAssignmentService(TaskService taskService, WorkerRegistryService workerRegistry,
                                  EventPublisher eventPublisher) {
        this.taskService = taskService;
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.assignment-interval-ms:1000}")
    public void assignPendingTasks() {
        for (TaskEnvelope task : taskService.getQueuedTasks()) {
            WorkerInfo worker = workerRegistry.nextWorkerRoundRobin();
            if (worker == null) {
                log.debug("No workers available, {} tasks remain queued", taskService.getQueuedTasks().size());
                return;
            }

            task.transitionTo(TaskState.ASSIGNED);

            var assignment = new TaskAssignment(
                task.getId(), task.getDescriptor(), worker.id(), Instant.now()
            );
            eventPublisher.publishMessage("tasks.assigned", worker.id(), assignment);

            eventPublisher.publishEvent("tasks.events", task.getId().toString(),
                new TaskAssignedEvent(
                    UUID.randomUUID().toString(), Instant.now(), task.getId(), worker.id()
                )
            );

            log.info("Assigned task {} to worker {}", task.getId(), worker.id());
        }
    }
}

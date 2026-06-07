package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Safety net for lost task assignments. An ASSIGNED task whose worker never
 * starts it (e.g. the assignment event was published before the worker's
 * consumer joined, a Kafka hiccup, or a worker crash between receipt and
 * state transition) would otherwise stay ASSIGNED forever: the worker-death
 * path only fires for workers that stop heartbeating, and the retry scanner
 * only looks at FAILED/TIMED_OUT.
 *
 * A healthy delivery leaves ASSIGNED within seconds (the worker reports
 * PROVISIONING on receipt), so anything older than the threshold is lost.
 */
@Service
public class StaleAssignmentScanner {

    private static final Logger log = LoggerFactory.getLogger(StaleAssignmentScanner.class);

    private final TaskRepository taskRepository;
    private final WorkerFailureHandler workerFailureHandler;
    private final long thresholdSeconds;

    public StaleAssignmentScanner(TaskRepository taskRepository,
                                  WorkerFailureHandler workerFailureHandler,
                                  @Value("${cloudbalancer.dispatcher.stale-assignment-threshold-seconds:60}") long thresholdSeconds) {
        this.taskRepository = taskRepository;
        this.workerFailureHandler = workerFailureHandler;
        this.thresholdSeconds = thresholdSeconds;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.stale-assignment-scan-interval-ms:15000}")
    public void scanForStaleAssignments() {
        Instant cutoff = Instant.now().minusSeconds(thresholdSeconds);
        var assignedTasks = taskRepository.findByStateIn(List.of(TaskState.ASSIGNED));

        for (TaskRecord task : assignedTasks) {
            Instant assignedAt = task.getAssignedAt();
            if (assignedAt == null || assignedAt.isBefore(cutoff)) {
                log.warn("Task {} stuck in ASSIGNED since {} (worker {}); re-queuing",
                    task.getId(), assignedAt, task.getAssignedWorkerId());
                workerFailureHandler.requeueInFlight(task,
                    "Assignment not picked up within " + thresholdSeconds + "s; re-queued");
            }
        }
    }
}

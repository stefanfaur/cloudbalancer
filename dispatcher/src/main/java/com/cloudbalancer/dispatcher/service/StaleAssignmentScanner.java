package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Safety net for genuinely lost task assignments. An ASSIGNED task whose worker
 * never receives it (e.g. the assignment event was published before the worker's
 * consumer joined, or a Kafka hiccup) would otherwise stay ASSIGNED forever: the
 * retry scanner only looks at FAILED/TIMED_OUT.
 *
 * <p>Re-queuing is gated on worker liveness. A worker that is still alive
 * (HEALTHY/RECOVERING, i.e. heartbeating) has not lost the assignment — it is
 * working through its backlog and will report RUNNING when it starts the task,
 * at which point the task leaves ASSIGNED and this scanner ignores it. Re-queuing
 * a task that a live worker still holds would spawn a duplicate execution (the
 * worker runs both the original and the re-dispatched copy), so we only recover
 * assignments whose worker is no longer alive. Tasks on a worker that stops
 * heartbeating are handled by the worker-death path ({@link WorkerFailureHandler#onWorkerDead}).
 */
@Service
public class StaleAssignmentScanner {

    private static final Logger log = LoggerFactory.getLogger(StaleAssignmentScanner.class);

    private final TaskRepository taskRepository;
    private final WorkerFailureHandler workerFailureHandler;
    private final WorkerRegistryService workerRegistry;
    private final long thresholdSeconds;

    public StaleAssignmentScanner(TaskRepository taskRepository,
                                  WorkerFailureHandler workerFailureHandler,
                                  WorkerRegistryService workerRegistry,
                                  @Value("${cloudbalancer.dispatcher.stale-assignment-threshold-seconds:60}") long thresholdSeconds) {
        this.taskRepository = taskRepository;
        this.workerFailureHandler = workerFailureHandler;
        this.workerRegistry = workerRegistry;
        this.thresholdSeconds = thresholdSeconds;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.stale-assignment-scan-interval-ms:15000}")
    public void scanForStaleAssignments() {
        Instant cutoff = Instant.now().minusSeconds(thresholdSeconds);
        var assignedTasks = taskRepository.findByStateIn(List.of(TaskState.ASSIGNED));

        for (TaskRecord task : assignedTasks) {
            Instant assignedAt = task.getAssignedAt();
            if (assignedAt != null && !assignedAt.isBefore(cutoff)) {
                continue; // still within the grace window
            }

            // A live worker has not lost the assignment — it is still going to
            // run it. Re-queuing now would duplicate a task that is in flight.
            if (isWorkerAlive(task.getAssignedWorkerId())) {
                log.debug("Task {} still ASSIGNED to live worker {} since {}; leaving in place",
                    task.getId(), task.getAssignedWorkerId(), assignedAt);
                continue;
            }

            log.warn("Task {} stuck in ASSIGNED since {} (worker {} not alive); re-queuing",
                task.getId(), assignedAt, task.getAssignedWorkerId());
            workerFailureHandler.requeueInFlight(task,
                "Assignment not picked up within " + thresholdSeconds + "s; re-queued");
        }
    }

    /**
     * A worker is "alive" — and therefore presumed to still hold and run its
     * assignment — when it exists and is HEALTHY or RECOVERING (heartbeating).
     * A worker that is absent, DEAD, SUSPECT, DRAINING, or STOPPING will not run
     * the task, so its assignments are safe to recover.
     */
    private boolean isWorkerAlive(String workerId) {
        if (workerId == null) {
            return false;
        }
        WorkerRecord worker = workerRegistry.getWorker(workerId);
        return worker != null
            && (worker.getHealthState() == WorkerHealthState.HEALTHY
             || worker.getHealthState() == WorkerHealthState.RECOVERING);
    }
}

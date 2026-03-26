package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HeartbeatTracker {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatTracker.class);

    private final WorkerRepository workerRepository;
    private final WorkerFailureHandler workerFailureHandler;
    private final ConcurrentHashMap<String, Instant> lastSeenAt = new ConcurrentHashMap<>();
    private final long suspectThresholdSeconds;
    private final long deadThresholdSeconds;

    public HeartbeatTracker(
            WorkerRepository workerRepository,
            WorkerFailureHandler workerFailureHandler,
            @Value("${cloudbalancer.dispatcher.heartbeat-suspect-threshold-seconds:30}") long suspectThresholdSeconds,
            @Value("${cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds:60}") long deadThresholdSeconds) {
        this.workerRepository = workerRepository;
        this.workerFailureHandler = workerFailureHandler;
        this.suspectThresholdSeconds = suspectThresholdSeconds;
        this.deadThresholdSeconds = deadThresholdSeconds;
    }

    public void onHeartbeat(String workerId, Instant timestamp) {
        lastSeenAt.put(workerId, timestamp);
        workerRepository.findById(workerId).ifPresent(worker -> {
            if (worker.getHealthState() == WorkerHealthState.SUSPECT) {
                worker.setHealthState(WorkerHealthState.HEALTHY);
                workerRepository.save(worker);
                log.info("Worker {} recovered: SUSPECT -> HEALTHY", workerId);
            }
            // RECOVERING workers: heartbeat is tracked but state promotion happens in checkLiveness()
        });
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.liveness-check-interval-ms:10000}")
    public void checkLiveness() {
        Instant now = Instant.now();
        for (WorkerRecord worker : workerRepository.findAll()) {
            // Promote RECOVERING -> HEALTHY after 60s
            if (worker.getHealthState() == WorkerHealthState.RECOVERING
                    && worker.getRecoveryStartedAt() != null) {
                long recoverySeconds = Duration.between(worker.getRecoveryStartedAt(), now).getSeconds();
                if (recoverySeconds >= 60) {
                    worker.setHealthState(WorkerHealthState.HEALTHY);
                    worker.setRecoveryStartedAt(null);
                    workerRepository.save(worker);
                    log.info("Worker {} promoted: RECOVERING -> HEALTHY after {}s", worker.getId(), recoverySeconds);
                    continue;  // Skip further checks for this worker
                }
            }

            Instant lastSeen = lastSeenAt.getOrDefault(worker.getId(), worker.getRegisteredAt());
            long secondsSinceLastSeen = Duration.between(lastSeen, now).getSeconds();

            if (secondsSinceLastSeen >= deadThresholdSeconds
                    && worker.getHealthState() != WorkerHealthState.DEAD) {
                boolean wasDraining = worker.getHealthState() == WorkerHealthState.DRAINING;
                worker.setHealthState(WorkerHealthState.DEAD);
                workerRepository.save(worker);
                log.info("Worker {} transitioned to DEAD ({}s since last seen)",
                        worker.getId(), secondsSinceLastSeen);
                if (!wasDraining) {
                    workerFailureHandler.onWorkerDead(worker.getId());
                } else {
                    log.info("Worker {} was DRAINING — skipping task re-queue", worker.getId());
                }
            } else if (secondsSinceLastSeen >= suspectThresholdSeconds
                    && worker.getHealthState() == WorkerHealthState.HEALTHY) {
                worker.setHealthState(WorkerHealthState.SUSPECT);
                workerRepository.save(worker);
                log.info("Worker {} transitioned to SUSPECT ({}s since last seen)",
                        worker.getId(), secondsSinceLastSeen);
            }
        }
    }

    ConcurrentHashMap<String, Instant> getLastSeenMap() {
        return lastSeenAt;
    }
}

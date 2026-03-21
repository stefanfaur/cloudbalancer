package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.TaskResult;
import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChaosMonkeyService {

    private static final Logger log = LoggerFactory.getLogger(ChaosMonkeyService.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final WorkerFailureHandler workerFailureHandler;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final AtomicReference<LatencyInjection> latencyInjection = new AtomicReference<>(null);

    public ChaosMonkeyService(WorkerRepository workerRepository,
                               TaskRepository taskRepository,
                               WorkerFailureHandler workerFailureHandler,
                               KafkaTemplate<String, String> kafkaTemplate) {
        this.workerRepository = workerRepository;
        this.taskRepository = taskRepository;
        this.workerFailureHandler = workerFailureHandler;
        this.kafkaTemplate = kafkaTemplate;
    }

    // --- Inner record DTOs ---

    public record ChaosKillResponse(String killedWorkerId, int orphanedTasks) {}

    public record ChaosFailResponse(UUID failedTaskId) {}

    public record ChaosLatencyResponse(String targetComponent, long delayMs, Instant expiresAt) {}

    public record LatencyInjection(String targetComponent, long delayMs, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    // --- Chaos operations ---

    /**
     * Kill a worker: mark it DEAD and trigger WorkerFailureHandler.
     * If no workerId is provided, a random HEALTHY worker is chosen.
     */
    public ChaosKillResponse killWorker(Optional<String> workerId) {
        WorkerRecord worker;
        if (workerId.isPresent()) {
            worker = workerRepository.findById(workerId.get())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + workerId.get()));
        } else {
            List<WorkerRecord> healthyWorkers = workerRepository.findByHealthState(WorkerHealthState.HEALTHY);
            if (healthyWorkers.isEmpty()) {
                throw new IllegalStateException("No HEALTHY workers available to kill");
            }
            worker = healthyWorkers.get(ThreadLocalRandom.current().nextInt(healthyWorkers.size()));
        }

        String targetWorkerId = worker.getId();
        log.warn("[ChaosMonkey] Killing worker {}", targetWorkerId);

        worker.setHealthState(WorkerHealthState.DEAD);
        workerRepository.save(worker);

        // Count orphaned tasks before the handler re-queues them
        List<TaskRecord> orphanedTasks = taskRepository.findByAssignedWorkerIdAndStateIn(
            targetWorkerId,
            List.of(TaskState.ASSIGNED, TaskState.PROVISIONING, TaskState.RUNNING)
        );

        workerFailureHandler.onWorkerDead(targetWorkerId);

        log.warn("[ChaosMonkey] Worker {} killed, {} tasks orphaned and re-queued",
            targetWorkerId, orphanedTasks.size());

        return new ChaosKillResponse(targetWorkerId, orphanedTasks.size());
    }

    /**
     * Fail a task: create a synthetic TaskResult with exitCode=1 and publish to tasks.results.
     * If no taskId is provided, a random RUNNING/ASSIGNED task is chosen.
     * Uses the task's actual currentExecutionId so the result passes idempotency checks.
     */
    public ChaosFailResponse failTask(Optional<UUID> taskId) {
        TaskRecord task;
        if (taskId.isPresent()) {
            task = taskRepository.findById(taskId.get())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId.get()));
        } else {
            List<TaskRecord> activeTasks = taskRepository.findByStateIn(
                List.of(TaskState.RUNNING, TaskState.ASSIGNED)
            );
            if (activeTasks.isEmpty()) {
                throw new IllegalStateException("No RUNNING/ASSIGNED tasks available to fail");
            }
            task = activeTasks.get(ThreadLocalRandom.current().nextInt(activeTasks.size()));
        }

        log.warn("[ChaosMonkey] Failing task {}", task.getId());

        TaskResult syntheticResult = new TaskResult(
            task.getId(),
            task.getAssignedWorkerId() != null ? task.getAssignedWorkerId() : "chaos-monkey",
            1,       // exitCode=1 means failure
            "",
            "ChaosMonkey: synthetic failure injected",
            0,
            false,
            Instant.now(),
            task.getCurrentExecutionId()  // Use actual executionId for idempotency
        );

        try {
            String json = JsonUtil.mapper().writeValueAsString(syntheticResult);
            kafkaTemplate.send("tasks.results", task.getId().toString(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize synthetic task result", e);
        }

        log.warn("[ChaosMonkey] Synthetic failure published for task {}", task.getId());

        return new ChaosFailResponse(task.getId());
    }

    /**
     * Inject latency into a component. The injection auto-expires after durationSeconds.
     */
    public ChaosLatencyResponse injectLatency(String component, long delayMs, int durationSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        LatencyInjection injection = new LatencyInjection(component, delayMs, expiresAt);
        latencyInjection.set(injection);

        log.warn("[ChaosMonkey] Injecting {}ms latency into '{}' for {}s (expires at {})",
            delayMs, component, durationSeconds, expiresAt);

        return new ChaosLatencyResponse(component, delayMs, expiresAt);
    }

    /**
     * Check if latency injection is active for the given component and apply it (sleep).
     */
    public void checkAndApplyLatency(String component) throws InterruptedException {
        LatencyInjection injection = latencyInjection.get();
        if (injection != null && !injection.isExpired() && component.equals(injection.targetComponent())) {
            log.debug("[ChaosMonkey] Applying {}ms latency to '{}'", injection.delayMs(), component);
            Thread.sleep(injection.delayMs());
        } else if (injection != null && injection.isExpired()) {
            latencyInjection.compareAndSet(injection, null);
        }
    }
}

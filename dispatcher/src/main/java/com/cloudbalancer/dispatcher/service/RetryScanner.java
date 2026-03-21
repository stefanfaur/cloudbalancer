package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.event.TaskDeadLetteredEvent;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetryScanner {

    private static final Logger log = LoggerFactory.getLogger(RetryScanner.class);
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final EventPublisher eventPublisher;
    private final long baseDelaySeconds;
    private final int poisonPillThresholdMs;

    public RetryScanner(TaskRepository taskRepository, TaskService taskService,
                       EventPublisher eventPublisher,
                       @Value("${cloudbalancer.retry.base-delay-seconds:5}") long baseDelaySeconds,
                       @Value("${cloudbalancer.retry.poison-pill-threshold-ms:2000}") int poisonPillThresholdMs) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.eventPublisher = eventPublisher;
        this.baseDelaySeconds = baseDelaySeconds;
        this.poisonPillThresholdMs = poisonPillThresholdMs;
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.retry.scan-interval-ms:5000}")
    public void scanAndRetry() {
        Instant now = Instant.now();
        var failedTasks = taskRepository.findByStateIn(List.of(TaskState.FAILED, TaskState.TIMED_OUT));

        for (TaskRecord task : failedTasks) {
            if (task.getRetryEligibleAt() != null && task.getRetryEligibleAt().isAfter(now)) {
                continue; // Backoff not yet elapsed
            }

            // Check failureAction first
            if (task.getDescriptor().executionPolicy().failureAction() == FailureAction.DEAD_LETTER) {
                deadLetter(task, "Failure action policy is DEAD_LETTER");
                continue;
            }

            // Check attempt count (only non-worker-caused attempts count)
            int attemptCount = countNonWorkerCausedAttempts(task);
            if (attemptCount >= task.getDescriptor().executionPolicy().maxRetries()) {
                deadLetter(task, "Exceeded max retries (" + attemptCount + "/" +
                    task.getDescriptor().executionPolicy().maxRetries() + ")");
                continue;
            }

            // Check poison pill
            if (isPoisonPill(task)) {
                deadLetter(task, "Poison pill detected: rapid failures on distinct workers");
                continue;
            }

            // Eligible for retry
            requeue(task);
        }
    }

    private int countNonWorkerCausedAttempts(TaskRecord task) {
        return (int) task.getExecutionHistory().stream()
            .filter(attempt -> !attempt.workerCausedFailure())
            .count();
    }

    private boolean isPoisonPill(TaskRecord task) {
        List<ExecutionAttempt> history = task.getExecutionHistory();
        if (history.size() < 3) return false;

        List<ExecutionAttempt> lastThree = history.subList(Math.max(0, history.size() - 3), history.size());

        // All must be fast failures (duration < poisonPillThresholdMs)
        boolean allFast = lastThree.stream().allMatch(a -> {
            long duration = Duration.between(a.startedAt(), a.completedAt()).toMillis();
            return duration < poisonPillThresholdMs;
        });
        if (!allFast) return false;

        // All must be on distinct workers
        Set<String> workers = lastThree.stream()
            .map(ExecutionAttempt::workerId)
            .collect(Collectors.toSet());
        return workers.size() >= 3;
    }

    private void requeue(TaskRecord task) {
        log.info("Re-queuing task {} (attempt {}/{})",
            task.getId(),
            countNonWorkerCausedAttempts(task) + 1,
            task.getDescriptor().executionPolicy().maxRetries());

        task.transitionTo(TaskState.QUEUED);
        task.setAssignedWorkerId(null);
        task.setCurrentExecutionId(UUID.randomUUID());
        task.setRetryEligibleAt(null);
        taskService.updateTask(task);
    }

    private void deadLetter(TaskRecord task, String reason) {
        log.info("Dead-lettering task {}: {}", task.getId(), reason);

        task.transitionTo(TaskState.DEAD_LETTERED);
        taskService.updateTask(task);

        var event = new TaskDeadLetteredEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            task.getId(),
            reason,
            task.getExecutionHistory().size(),
            task.getExecutionHistory()
        );
        eventPublisher.publishEvent("tasks.deadletter", task.getId().toString(), event);
    }
}

package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.ScalingEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.model.TaskState;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.config.ScalingProperties;
import com.cloudbalancer.dispatcher.scaling.AgentRuntime;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AutoScalerService {

    private static final Logger log = LoggerFactory.getLogger(AutoScalerService.class);

    private final NodeRuntime nodeRuntime;
    private final WorkerRegistryService workerRegistry;
    private final ScalingPolicyService policyService;
    private final EventPublisher eventPublisher;
    private final ScalingProperties props;
    private final TaskRepository taskRepository;

    private final ConcurrentLinkedDeque<MetricsSample> metricsWindow = new ConcurrentLinkedDeque<>();
    private final AtomicInteger autoWorkerCounter = new AtomicInteger(0);

    // Queue-pressure tracking: sliding counters for submission and completion rates
    private final AtomicLong submittedCounter = new AtomicLong(0);
    private final AtomicLong completedCounter = new AtomicLong(0);
    private final ConcurrentLinkedDeque<RateSnapshot> rateWindow = new ConcurrentLinkedDeque<>();

    private volatile Instant lastScalingAction = Instant.EPOCH;
    private volatile ScalingDecision lastDecision;
    private volatile Instant queueEmptySince;

    // Test offset for advancing the window
    private volatile Duration testTimeOffset = Duration.ZERO;

    public AutoScalerService(NodeRuntime nodeRuntime,
                             WorkerRegistryService workerRegistry,
                             ScalingPolicyService policyService,
                             EventPublisher eventPublisher,
                             ScalingProperties props,
                             TaskRepository taskRepository) {
        this.nodeRuntime = nodeRuntime;
        this.workerRegistry = workerRegistry;
        this.policyService = policyService;
        this.eventPublisher = eventPublisher;
        this.props = props;
        this.taskRepository = taskRepository;
    }

    public void recordMetrics(String workerId, double cpuPercent) {
        metricsWindow.addLast(new MetricsSample(effectiveNow(), cpuPercent, workerId));
    }

    public void recordTaskSubmitted() {
        submittedCounter.incrementAndGet();
    }

    public void recordTaskCompleted() {
        completedCounter.incrementAndGet();
    }

    @Scheduled(fixedDelayString = "${cloudbalancer.dispatcher.scaling.evaluation-interval-ms:30000}")
    public void scheduledEvaluate() {
        if (!props.isEnabled()) return;

        updateQueueEmptySince();
        snapshotRates();
        evaluate();
    }

    private void updateQueueEmptySince() {
        long queuedCount = taskRepository.countByState(TaskState.QUEUED);
        if (queuedCount == 0) {
            if (queueEmptySince == null) {
                queueEmptySince = effectiveNow();
            }
        } else {
            queueEmptySince = null;
        }
    }

    private void snapshotRates() {
        Instant now = effectiveNow();
        long submitted = submittedCounter.get();
        long completed = completedCounter.get();
        rateWindow.addLast(new RateSnapshot(now, submitted, completed));

        // Prune old snapshots
        Instant cutoff = now.minus(Duration.ofSeconds(props.getQueuePressureWindowSeconds()));
        while (!rateWindow.isEmpty() && rateWindow.peekFirst().timestamp().isBefore(cutoff)) {
            rateWindow.pollFirst();
        }
    }

    public void evaluate() {
        if (!props.isEnabled()) return;

        var policy = policyService.getCurrentPolicy();
        Instant now = effectiveNow();

        pruneWindow(now);

        if (metricsWindow.isEmpty()) return;

        var allWorkers = workerRegistry.getAllWorkers();
        var availableWorkers = workerRegistry.getAvailableWorkers();
        int currentCount = availableWorkers.size();
        if (nodeRuntime instanceof AgentRuntime agentRuntime) {
            currentCount += agentRuntime.getPendingWorkerCount();
        }

        // Check cooldown
        long cooldownRemaining = Duration.between(lastScalingAction, now).compareTo(policy.cooldownPeriod()) < 0
            ? policy.cooldownPeriod().minus(Duration.between(lastScalingAction, now)).getSeconds()
            : 0;

        if (cooldownRemaining > 0) {
            log.debug("Cooldown active: {}s remaining", cooldownRemaining);
            return;
        }

        double avgCpu = metricsWindow.stream()
            .mapToDouble(MetricsSample::cpuPercent)
            .average()
            .orElse(0.0);

        // Reactive scale-up: avg CPU > high threshold
        if (avgCpu >= props.getCpuHighThreshold() && currentCount < policy.maxWorkers()) {
            int step = computeScaleUpStep(avgCpu);
            int toAdd = Math.min(step, policy.maxWorkers() - currentCount);
            if (toAdd > 0) {
                scaleUp(toAdd, ScalingTriggerType.REACTIVE,
                    String.format("Avg CPU %.1f%% > %.1f%% threshold for %ds window",
                        avgCpu, props.getCpuHighThreshold(), props.getReactiveWindowSeconds()),
                    currentCount, policy);
                return;
            }
        }

        // Queue-pressure scale-up: submission rate outpacing completion rate
        if (currentCount < policy.maxWorkers() && rateWindow.size() >= 2) {
            RateSnapshot oldest = rateWindow.peekFirst();
            RateSnapshot newest = rateWindow.peekLast();
            long submittedDelta = newest.totalSubmitted() - oldest.totalSubmitted();
            long completedDelta = newest.totalCompleted() - oldest.totalCompleted();

            if (completedDelta > 0) {
                double pressureRatio = (double) submittedDelta / completedDelta;
                if (pressureRatio >= props.getQueuePressureRatioThreshold()) {
                    int toAdd = Math.min(1, policy.maxWorkers() - currentCount);
                    if (toAdd > 0) {
                        scaleUp(toAdd, ScalingTriggerType.QUEUE_PRESSURE,
                            String.format("Queue pressure ratio %.2f >= %.2f threshold over %ds window",
                                pressureRatio, props.getQueuePressureRatioThreshold(),
                                props.getQueuePressureWindowSeconds()),
                            currentCount, policy);
                        return;
                    }
                }
            } else if (submittedDelta > 0) {
                // Tasks being submitted but none completing — pressure is high
                int toAdd = Math.min(1, policy.maxWorkers() - currentCount);
                if (toAdd > 0) {
                    scaleUp(toAdd, ScalingTriggerType.QUEUE_PRESSURE,
                        String.format("Queue pressure: %d submitted, 0 completed over %ds window",
                            submittedDelta, props.getQueuePressureWindowSeconds()),
                        currentCount, policy);
                    return;
                }
            }
        }

        // Scale-down: avg CPU < low threshold for extended window AND queue empty
        if (avgCpu <= props.getCpuLowThreshold() && currentCount > policy.minWorkers()) {
            boolean windowLongEnough = hasAllSamplesBelowThresholdFor(
                props.getCpuLowThreshold(), Duration.ofSeconds(props.getScaleDownWindowSeconds()), now);
            boolean queueEmpty = queueEmptySince != null
                && Duration.between(queueEmptySince, now).getSeconds() >= props.getScaleDownWindowSeconds();

            if (windowLongEnough && queueEmpty) {
                int toRemove = Math.min(policy.scaleDownStep(), currentCount - policy.minWorkers());
                if (toRemove > 0) {
                    scaleDown(toRemove, availableWorkers,
                        String.format("Avg CPU %.1f%% < %.1f%% for %ds, queue empty",
                            avgCpu, props.getCpuLowThreshold(), props.getScaleDownWindowSeconds()));
                }
            }
        }
    }

    public ScalingDecision triggerManual(ScalingAction action, int count) {
        var policy = policyService.getCurrentPolicy();
        var available = workerRegistry.getAvailableWorkers();
        int currentCount = available.size();

        if (action == ScalingAction.SCALE_UP) {
            int toAdd = Math.min(count, policy.maxWorkers() - currentCount);
            if (toAdd > 0) {
                return scaleUp(toAdd, ScalingTriggerType.MANUAL, "Manual trigger", currentCount, policy);
            }
        } else if (action == ScalingAction.SCALE_DOWN) {
            int toRemove = Math.min(count, currentCount - policy.minWorkers());
            if (toRemove > 0) {
                return scaleDown(toRemove, available, "Manual trigger");
            }
        }

        var decision = new ScalingDecision(ScalingAction.NONE,
            "No action: bounds check prevented scaling", ScalingTriggerType.MANUAL,
            currentCount, currentCount, Instant.now());
        this.lastDecision = decision;
        return decision;
    }

    public ScalingDecision getLastDecision() {
        return lastDecision;
    }

    public long getCooldownRemainingSeconds() {
        var policy = policyService.getCurrentPolicy();
        Instant now = effectiveNow();
        long elapsed = Duration.between(lastScalingAction, now).getSeconds();
        long cooldown = policy.cooldownPeriod().getSeconds();
        return Math.max(0, cooldown - elapsed);
    }

    // --- Private helpers ---

    private Set<ExecutorType> parseExecutorTypes() {
        return Arrays.stream(props.getDefaultWorkerExecutorTypes().split(","))
            .map(String::trim)
            .map(ExecutorType::valueOf)
            .collect(Collectors.toSet());
    }

    private int computeScaleUpStep(double avgCpu) {
        if (avgCpu > 95.0) return 3;
        if (avgCpu > 90.0) return 2;
        return 1;
    }

    private ScalingDecision scaleUp(int count, ScalingTriggerType triggerType, String reason,
                                     int currentCount, ScalingPolicy policy) {
        List<String> added = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String workerId = "auto-docker-" + autoWorkerCounter.incrementAndGet();
            var config = new WorkerConfig(workerId, parseExecutorTypes(),
                props.getDefaultWorkerCpuCores(), props.getDefaultWorkerMemoryMb(),
                props.getDefaultWorkerDiskMb(), Set.of());
            boolean ok = nodeRuntime.startWorker(config);
            if (ok) {
                added.add(workerId);
                log.info("Scaled up: started worker {}", workerId);
            }
        }

        int newCount = currentCount + added.size();
        var decision = new ScalingDecision(ScalingAction.SCALE_UP, reason, triggerType,
            currentCount, newCount, Instant.now());
        this.lastDecision = decision;
        this.lastScalingAction = effectiveNow();

        var event = new ScalingEvent(UUID.randomUUID().toString(), Instant.now(),
            ScalingAction.SCALE_UP, triggerType, reason,
            currentCount, newCount, added, List.of());
        eventPublisher.publishEvent("system.scaling", "scaling", event);

        log.info("Scale-up complete: {} -> {} workers ({})", currentCount, newCount, reason);
        return decision;
    }

    private ScalingDecision scaleDown(int count, List<?> availableWorkers, String reason) {
        int currentCount = availableWorkers.size();
        List<String> removed = new ArrayList<>();

        for (int i = 0; i < count && i < availableWorkers.size(); i++) {
            // Drain the last worker (least recently added)
            var worker = availableWorkers.get(availableWorkers.size() - 1 - i);
            String workerId;
            if (worker instanceof com.cloudbalancer.dispatcher.persistence.WorkerRecord wr) {
                workerId = wr.getId();
            } else {
                continue;
            }
            workerRegistry.drainWorker(workerId);
            nodeRuntime.drainAndStop(workerId, props.getDrainTimeSeconds());
            removed.add(workerId);
            log.info("Scale-down: draining worker {}", workerId);
        }

        int newCount = currentCount - removed.size();
        var decision = new ScalingDecision(ScalingAction.SCALE_DOWN, reason, ScalingTriggerType.REACTIVE,
            currentCount, newCount, Instant.now());
        this.lastDecision = decision;
        this.lastScalingAction = effectiveNow();

        var event = new ScalingEvent(UUID.randomUUID().toString(), Instant.now(),
            ScalingAction.SCALE_DOWN, ScalingTriggerType.REACTIVE, reason,
            currentCount, newCount, List.of(), removed);
        eventPublisher.publishEvent("system.scaling", "scaling", event);

        log.info("Scale-down complete: {} -> {} workers ({})", currentCount, newCount, reason);
        return decision;
    }

    private void pruneWindow(Instant now) {
        Instant cutoff = now.minus(Duration.ofSeconds(
            Math.max(props.getReactiveWindowSeconds(), props.getScaleDownWindowSeconds())));
        while (!metricsWindow.isEmpty() && metricsWindow.peekFirst().timestamp().isBefore(cutoff)) {
            metricsWindow.pollFirst();
        }
    }

    private boolean hasAllSamplesBelowThresholdFor(double threshold, Duration window, Instant now) {
        Instant windowStart = now.minus(window);
        return metricsWindow.stream()
            .filter(s -> s.timestamp().isAfter(windowStart))
            .allMatch(s -> s.cpuPercent() <= threshold);
    }

    private Instant effectiveNow() {
        return Instant.now().plus(testTimeOffset);
    }

    // --- Test helpers (package-private) ---

    public void resetForTest() {
        metricsWindow.clear();
        rateWindow.clear();
        submittedCounter.set(0);
        completedCounter.set(0);
        lastScalingAction = Instant.EPOCH;
        lastDecision = null;
        queueEmptySince = null;
        testTimeOffset = Duration.ZERO;
    }

    public void advanceWindowForTest(Duration offset) {
        this.testTimeOffset = offset;
    }

    public void setQueueEmptySince(Instant since) {
        this.queueEmptySince = since;
    }

    record MetricsSample(Instant timestamp, double cpuPercent, String workerId) {}
    record RateSnapshot(Instant timestamp, long totalSubmitted, long totalCompleted) {}
}

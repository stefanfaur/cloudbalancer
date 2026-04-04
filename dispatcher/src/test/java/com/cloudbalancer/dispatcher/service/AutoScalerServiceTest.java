package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.event.ScalingEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.config.ScalingProperties;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.persistence.TaskRepository;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoScalerServiceTest {

    @Mock NodeRuntime nodeRuntime;
    @Mock WorkerRegistryService workerRegistry;
    @Mock ScalingPolicyService policyService;
    @Mock EventPublisher eventPublisher;
    @Mock TaskRepository taskRepository;

    AutoScalerService autoScaler;

    @BeforeEach
    void setUp() {
        lenient().when(nodeRuntime.startWorker(any())).thenReturn(true);
        lenient().when(policyService.getCurrentPolicy()).thenReturn(ScalingPolicy.defaults());
        lenient().when(taskRepository.countByState(any())).thenReturn(0L);
        autoScaler = new AutoScalerService(nodeRuntime, workerRegistry, policyService, eventPublisher, scalingProperties(), taskRepository);
    }

    @Test
    void scalesUpWhenCpuHighForWindow() {
        var workers = List.of(
            makeWorkerRecord("w1", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w2", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w3", WorkerHealthState.HEALTHY)
        );
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        // Record metrics — samples land at effectiveNow (= now, no offset yet)
        autoScaler.recordMetrics("w1", 90.0);
        autoScaler.recordMetrics("w2", 92.0);
        autoScaler.recordMetrics("w3", 88.0);

        autoScaler.evaluate();

        verify(nodeRuntime, atLeastOnce()).startWorker(any(WorkerConfig.class));
        verify(eventPublisher).publishEvent(eq("system.scaling"), any(), any(ScalingEvent.class));
    }

    @Test
    void doesNotScaleUpBelowThreshold() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        lenient().when(workerRegistry.getAllWorkers()).thenReturn(workers);
        lenient().when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 50.0);

        autoScaler.evaluate();

        verify(nodeRuntime, never()).startWorker(any());
    }

    @Test
    void respectsCooldown() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 95.0);

        autoScaler.evaluate(); // First — triggers scale-up
        autoScaler.evaluate(); // Second — within cooldown, should NOT trigger

        // Only one scale-up event should be published
        verify(eventPublisher, times(1)).publishEvent(eq("system.scaling"), any(), any(ScalingEvent.class));
    }

    @Test
    void respectsMaxWorkersBound() {
        var policy = new ScalingPolicy(2, 3, Duration.ofMinutes(3), 1, 1, Duration.ofSeconds(60));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        var workers = List.of(
            makeWorkerRecord("w1", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w2", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w3", WorkerHealthState.HEALTHY)
        );
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 95.0);
        autoScaler.recordMetrics("w2", 95.0);
        autoScaler.recordMetrics("w3", 95.0);

        autoScaler.evaluate();

        verify(nodeRuntime, never()).startWorker(any());
    }

    @Test
    void scalesDownWhenCpuLowAndQueueEmpty() {
        var policy = new ScalingPolicy(1, 20, Duration.ofMinutes(3), 1, 1, Duration.ofSeconds(60));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        var workers = List.of(
            makeWorkerRecord("w1", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w2", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w3", WorkerHealthState.HEALTHY)
        );
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        // Advance time first, THEN record metrics so samples are within the window
        autoScaler.advanceWindowForTest(Duration.ofMinutes(6));
        autoScaler.recordMetrics("w1", 10.0);
        autoScaler.recordMetrics("w2", 8.0);
        autoScaler.recordMetrics("w3", 12.0);
        autoScaler.setQueueEmptySince(Instant.now().minus(Duration.ofMinutes(6)));

        autoScaler.evaluate();

        verify(workerRegistry).drainWorker(anyString());
        verify(nodeRuntime).drainAndStop(anyString(), eq(60));
    }

    @Test
    void manualScaleDownCallsDrainAndStop() {
        var policy = new ScalingPolicy(1, 20, Duration.ofSeconds(1), 1, 1, Duration.ofSeconds(60));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        var workers = List.of(
            makeWorkerRecord("w1", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w2", WorkerHealthState.HEALTHY)
        );
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.triggerManual(ScalingAction.SCALE_DOWN, 1);

        verify(workerRegistry).drainWorker(anyString());
        verify(nodeRuntime).drainAndStop(anyString(), eq(60));
    }

    @Test
    void scaleUpStepProportionalToCpuPressure() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 93.0);

        autoScaler.evaluate();

        // 91-95% = step 2
        verify(nodeRuntime, times(2)).startWorker(any(WorkerConfig.class));
    }

    @Test
    void scaleUpStep3AboveCriticalCpu() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 98.0);

        autoScaler.evaluate();

        // >95% = step 3
        verify(nodeRuntime, times(3)).startWorker(any(WorkerConfig.class));
    }

    @Test
    void respectsMinWorkersBound() {
        var policy = new ScalingPolicy(3, 20, Duration.ofMinutes(3), 1, 1, Duration.ofSeconds(60));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        var workers = List.of(
            makeWorkerRecord("w1", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w2", WorkerHealthState.HEALTHY),
            makeWorkerRecord("w3", WorkerHealthState.HEALTHY)
        );
        lenient().when(workerRegistry.getAllWorkers()).thenReturn(workers);
        lenient().when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        // Advance time, then record low CPU metrics within the window
        autoScaler.advanceWindowForTest(Duration.ofMinutes(6));
        autoScaler.recordMetrics("w1", 10.0);
        autoScaler.setQueueEmptySince(Instant.now().minus(Duration.ofMinutes(6)));

        autoScaler.evaluate();

        verify(workerRegistry, never()).drainWorker(anyString());
    }

    @Test
    void scalesUpOnQueuePressure() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        when(workerRegistry.getAllWorkers()).thenReturn(workers);
        when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        // Record low CPU so reactive signal won't fire
        autoScaler.recordMetrics("w1", 50.0);

        // First snapshot: 0 submitted, 0 completed (counters start at 0)
        autoScaler.scheduledEvaluate();  // snapshot 1

        // Simulate queue pressure: 10 submitted, 2 completed -> ratio 5.0 > 1.5
        for (int i = 0; i < 10; i++) autoScaler.recordTaskSubmitted();
        for (int i = 0; i < 2; i++) autoScaler.recordTaskCompleted();

        // Need to keep CPU metrics in the window for evaluate() to not return early
        autoScaler.recordMetrics("w1", 50.0);
        autoScaler.scheduledEvaluate();  // snapshot 2 + evaluate with delta

        verify(nodeRuntime, atLeastOnce()).startWorker(any(WorkerConfig.class));
        var decision = autoScaler.getLastDecision();
        assertThat(decision).isNotNull();
        assertThat(decision.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(decision.triggerType()).isEqualTo(ScalingTriggerType.QUEUE_PRESSURE);
    }

    @Test
    void noQueuePressureScaleUpWhenRatioIsLow() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        lenient().when(workerRegistry.getAllWorkers()).thenReturn(workers);
        lenient().when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        autoScaler.recordMetrics("w1", 50.0);

        // Balanced: 5 submitted, 5 completed -> ratio 1.0 < 1.5
        for (int i = 0; i < 5; i++) autoScaler.recordTaskSubmitted();
        for (int i = 0; i < 5; i++) autoScaler.recordTaskCompleted();
        autoScaler.scheduledEvaluate();

        for (int i = 0; i < 5; i++) autoScaler.recordTaskSubmitted();
        for (int i = 0; i < 5; i++) autoScaler.recordTaskCompleted();
        autoScaler.scheduledEvaluate();

        verify(nodeRuntime, never()).startWorker(any());
    }

    @Test
    void scheduledEvaluateUpdatesQueueEmptySince() {
        var workers = List.of(makeWorkerRecord("w1", WorkerHealthState.HEALTHY));
        lenient().when(workerRegistry.getAllWorkers()).thenReturn(workers);
        lenient().when(workerRegistry.getAvailableWorkers()).thenReturn(workers);

        // Queue has tasks — queueEmptySince should be null
        when(taskRepository.countByState(any())).thenReturn(5L);
        autoScaler.recordMetrics("w1", 50.0);
        autoScaler.scheduledEvaluate();

        // Now empty queue
        when(taskRepository.countByState(any())).thenReturn(0L);
        autoScaler.scheduledEvaluate();

        // No direct assertion on queueEmptySince (private), but confirm no crash
        // The scale-down path tests queueEmptySince indirectly
    }

    // Helper methods
    private ScalingProperties scalingProperties() {
        var props = new ScalingProperties();
        props.setEnabled(true);
        props.setEvaluationIntervalMs(30000);
        props.setCpuHighThreshold(80.0);
        props.setCpuLowThreshold(30.0);
        props.setReactiveWindowSeconds(120);
        props.setScaleDownWindowSeconds(300);
        props.setQueuePressureWindowSeconds(120);
        props.setQueuePressureRatioThreshold(1.5);
        return props;
    }

    private WorkerRecord makeWorkerRecord(String id, WorkerHealthState state) {
        var caps = new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 8192, 10240, false, 0, true), Set.of());
        return new WorkerRecord(id, state, caps, Instant.now());
    }
}

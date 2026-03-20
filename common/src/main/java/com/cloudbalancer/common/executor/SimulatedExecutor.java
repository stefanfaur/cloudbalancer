package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.SecurityLevel;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SimulatedExecutor implements TaskExecutor {

    @Override
    public ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context) {
        int durationMs = ((Number) spec.getOrDefault("durationMs", 1000)).intValue();
        double failProbability = ((Number) spec.getOrDefault("failProbability", 0.0)).doubleValue();

        long start = System.currentTimeMillis();
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - start;
            return new ExecutionResult(1, "", "Execution interrupted (timeout)", elapsed, true);
        }
        long elapsed = System.currentTimeMillis() - start;

        if (ThreadLocalRandom.current().nextDouble() < failProbability) {
            return new ExecutionResult(1, "", "Simulated failure (probability=" + failProbability + ")", elapsed, false);
        }

        return new ExecutionResult(0, "simulated output for task " + context.taskId(), "", elapsed, false);
    }

    @Override
    public ValidationResult validate(Map<String, Object> spec) {
        if (!spec.containsKey("durationMs")) {
            return ValidationResult.invalid("Missing required field: durationMs");
        }

        int durationMs = ((Number) spec.get("durationMs")).intValue();
        if (durationMs < 0) {
            return ValidationResult.invalid("durationMs must be non-negative, got: " + durationMs);
        }

        if (spec.containsKey("failProbability")) {
            double failProb = ((Number) spec.get("failProbability")).doubleValue();
            if (failProb < 0.0 || failProb > 1.0) {
                return ValidationResult.invalid("failProbability must be between 0.0 and 1.0, got: " + failProb);
            }
        }

        return ValidationResult.ok();
    }

    @Override
    public ResourceEstimate estimateResources(Map<String, Object> spec) {
        int durationMs = ((Number) spec.getOrDefault("durationMs", 1000)).intValue();
        return new ResourceEstimate(1, 256, durationMs);
    }

    @Override
    public ExecutorCapabilities getCapabilities() {
        return new ExecutorCapabilities(
            false,
            false,
            new ResourceProfile(8, 16384, 51200, false, 3600, false),
            SecurityLevel.SANDBOXED
        );
    }

    @Override
    public ExecutorType getExecutorType() {
        return ExecutorType.SIMULATED;
    }

    @Override
    public void cancel(ExecutionHandle handle) {
        // No-op for stub. Phase 2 adds real cancellation.
    }
}

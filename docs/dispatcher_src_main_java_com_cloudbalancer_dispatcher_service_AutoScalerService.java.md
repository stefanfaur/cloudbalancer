# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/AutoScalerService.java

## Overview

`AutoScalerService` is the central decision-making engine for the CloudBalancer cluster, responsible for dynamic resource allocation. It monitors worker CPU utilization and task queue pressure to trigger horizontal scaling actions (scale-up or scale-down).

**Warning**: This file is a **critical hotspot** within the `dispatcher` module. It exhibits high change frequency and cyclomatic complexity. Modifications to scaling logic, threshold calculations, or windowing algorithms carry a high risk of introducing instability or "flapping" behavior in the cluster.

The service operates on a sliding window mechanism for both metrics and queue rates, ensuring that scaling decisions are based on sustained trends rather than transient spikes.

## Public API

### Core Scaling Logic
*   `recordMetrics(String workerId, double cpuPercent)`: Ingests CPU telemetry from a specific worker into the sliding window.
*   `recordTaskSubmitted()` / `recordTaskCompleted()`: Increments atomic counters used to calculate queue pressure.
*   `evaluate()`: Executes the primary scaling algorithm. It checks cooldowns, calculates average CPU, evaluates queue pressure ratios, and determines if a scale-up or scale-down action is required based on `ScalingPolicyService` configurations.
*   `scheduledEvaluate()`: A `@Scheduled` task that triggers `evaluate()` at intervals defined by `cloudbalancer.dispatcher.scaling.evaluation-interval-ms`.

### Manual Control
*   `triggerManual(ScalingAction action, int count)`: Forces a manual scale-up or scale-down operation, bypassing automated thresholds but respecting hard limits (min/max workers).
*   `triggerManualTargeted(ScalingAction action, int count, String agentId)`: Similar to `triggerManual`, but restricts the operation to a specific agent node.

### State & Diagnostics
*   `getLastDecision()`: Returns the most recent `ScalingDecision` object.
*   `getCooldownRemainingSeconds()`: Returns the time remaining in the current cooldown period before another action can be taken.

## Dependencies

*   **`NodeRuntime`**: Interface for interacting with the underlying infrastructure (e.g., starting/draining workers).
*   **`WorkerRegistryService`**: Tracks the state and availability of workers in the cluster.
*   **`ScalingPolicyService`**: Provides the current operational constraints (min/max workers, thresholds, cooldowns).
*   **`EventPublisher`**: Broadcasts `ScalingEvent` objects to the system bus for auditing and monitoring.
*   **`ScalingProperties`**: Configuration properties (e.g., `cpuHighThreshold`, `queuePressureWindowSeconds`).
*   **`TaskRepository`**: Used to check the current number of `QUEUED` tasks.

## Usage Notes

### Scaling Strategy
1.  **Reactive Scale-Up**: Triggered when the average CPU utilization across the `metricsWindow` exceeds `cpuHighThreshold`. The number of workers to add is computed via `computeScaleUpStep` (1 to 3 workers depending on severity).
2.  **Queue-Pressure Scale-Up**: Triggered when the ratio of submitted tasks to completed tasks exceeds `queuePressureRatioThreshold`. This is a proactive measure to prevent queue buildup before CPU saturation occurs.
3.  **Scale-Down**: Triggered only when **both** conditions are met:
    *   Average CPU is below `cpuLowThreshold` for the duration of `scaleDownWindowSeconds`.
    *   The task queue has been empty for at least `scaleDownWindowSeconds`.

### Implementation Pitfalls
*   **Cooldowns**: The service enforces a `cooldownPeriod` defined in the `ScalingPolicy`. If an action was recently performed, `evaluate()` will return early.
*   **Window Pruning**: The `metricsWindow` and `rateWindow` are pruned during every `evaluate()` call. Ensure that the `evaluation-interval-ms` is significantly smaller than the window durations to maintain accurate averages.
*   **Testability**: The service includes `resetForTest`, `advanceWindowForTest`, and `setQueueEmptySince` methods. These are intended for integration testing and should not be invoked in production environments.

### Example: Manual Scaling
To force an immediate scale-up of 2 workers via the API:
```java
// Assuming autoScalerService is injected
autoScalerService.triggerManual(ScalingAction.SCALE_UP, 2);
```

### Example: Monitoring
To check if the system is currently in a cooldown state:
```java
long remaining = autoScalerService.getCooldownRemainingSeconds();
if (remaining > 0) {
    System.out.println("System is cooling down for " + remaining + " seconds.");
}
```
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/WeightedRoundRobinStrategy.java

## Overview

The `WeightedRoundRobinStrategy` is an implementation of the `SchedulingStrategy` interface that distributes tasks among available workers based on their resource capacity. By calculating a weight for each worker derived from their CPU, memory, and disk capabilities, this strategy ensures that more powerful nodes receive a proportionally higher volume of assignments compared to smaller nodes.

## Public API

### `select(TaskRecord task, List<WorkerRecord> candidates, Map<String, WorkerScorer> scorers)`
Selects an optimal `WorkerRecord` from the provided list of candidates.
*   **Parameters**:
    *   `task`: The `TaskRecord` to be scheduled.
    *   `candidates`: A list of available `WorkerRecord` instances.
    *   `scorers`: A map of `WorkerScorer` instances for additional evaluation (note: currently unused in this implementation).
*   **Returns**: An `Optional<WorkerRecord>` containing the selected worker, or `Optional.empty()` if no candidates are provided.

### `getName()`
Returns the unique identifier for this strategy.
*   **Returns**: `"WEIGHTED_ROUND_ROBIN"`

### `getWeights()`
Provides the current weight configuration.
*   **Returns**: An empty `Map<String, Integer>` as weights are calculated dynamically per request based on worker capabilities.

## Dependencies

*   `com.cloudbalancer.common.model.ResourceProfile`: Used to extract CPU, memory, and disk metrics for weight calculation.
*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task being scheduled.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the target worker nodes.
*   `java.util.concurrent.atomic.AtomicLong`: Used to maintain a thread-safe global counter for the round-robin rotation.

## Usage Notes

*   **Weight Calculation**: The strategy calculates weight using the formula: `cpuCores + (memoryMB / 256) + (diskMB / 256)`. Each worker is guaranteed a minimum weight of 1 to prevent starvation of nodes with minimal reported resources.
*   **Thread Safety**: The strategy uses an `AtomicLong` counter, making it safe for concurrent use across multiple threads in the dispatcher.
*   **Dynamic Nature**: Because weights are calculated on-the-fly based on the `candidates` list provided during each `select` call, this strategy automatically adapts to changes in the cluster composition (e.g., workers joining or leaving).
*   **Scorer Integration**: While the `select` method signature accepts `WorkerScorer` instances, this specific implementation currently ignores them, relying solely on the capacity-based weighted round-robin logic.
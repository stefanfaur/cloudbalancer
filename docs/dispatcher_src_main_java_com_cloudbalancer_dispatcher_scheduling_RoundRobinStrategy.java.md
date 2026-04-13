# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/RoundRobinStrategy.java

## Overview

The `RoundRobinStrategy` class implements the `SchedulingStrategy` interface to provide a simple, deterministic load-balancing mechanism. It distributes incoming tasks across a list of available `WorkerRecord` instances in a cyclic order. This strategy is stateless regarding worker performance or load, ensuring an equal distribution of tasks based on the sequence of arrival.

## Public API

### `RoundRobinStrategy`
The primary class for the Round Robin scheduling implementation.

### `select(TaskRecord task, List<WorkerRecord> candidates, Map<String, WorkerScorer> scorers)`
Selects a worker from the provided list of candidates.
- **Parameters**:
    - `task`: The `TaskRecord` to be scheduled.
    - `candidates`: A `List` of available `WorkerRecord` objects.
    - `scorers`: A map of `WorkerScorer` instances (unused in this implementation).
- **Returns**: An `Optional<WorkerRecord>` containing the selected worker, or `Optional.empty()` if the candidate list is empty.

### `getName()`
Returns the identifier for this strategy.
- **Returns**: `"ROUND_ROBIN"`

### `getWeights()`
Returns the weight configuration for the strategy.
- **Returns**: An empty `Map<String, Integer>`, as this strategy does not utilize weighted distribution.

## Dependencies

- `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task being scheduled.
- `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker node to which the task is assigned.
- `java.util.List`: Used to manage the collection of candidate workers.
- `java.util.Map`: Used for the scoring interface (unused).
- `java.util.Optional`: Used for safe return types when selecting workers.
- `java.util.concurrent.atomic.AtomicInteger`: Used to maintain thread-safe state for the cyclic index.

## Usage Notes

- **Thread Safety**: This implementation uses an `AtomicInteger` to track the current index, making it safe for concurrent use across multiple threads when selecting workers.
- **Cyclic Behavior**: The strategy uses `Math.floorMod` to ensure the index wraps around correctly even if the `AtomicInteger` overflows, guaranteeing a continuous cycle through the provided `candidates` list.
- **Performance**: This is an O(1) operation (excluding list access), making it highly efficient for scenarios where task distribution overhead must be minimized.
- **Limitations**: Because this strategy does not account for worker health, current load, or capacity, it is best suited for environments where all workers are homogeneous and have identical processing capabilities.
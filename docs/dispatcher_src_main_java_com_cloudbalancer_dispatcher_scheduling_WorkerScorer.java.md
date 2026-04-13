# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/WorkerScorer.java

## Overview

The `WorkerScorer` interface defines the contract for components responsible for evaluating the suitability of a specific `WorkerRecord` for a given `TaskRecord`. Implementations of this interface are used by the dispatcher to rank available workers, enabling intelligent task distribution across the CloudBalancer infrastructure.

## Public API

### `WorkerScorer`

The interface provides the following methods for worker evaluation:

#### `int score(TaskRecord task, WorkerRecord worker)`
Calculates a suitability score for the provided worker relative to the given task.
*   **Parameters**: 
    *   `task`: The `TaskRecord` representing the work to be performed.
    *   `worker`: The `WorkerRecord` representing the candidate node.
*   **Returns**: An integer score between 0 and 100, where higher values indicate better suitability.

#### `String getScorerName()`
Returns the unique identifier or descriptive name of the scoring strategy.
*   **Returns**: A `String` representing the name of the scorer.

## Dependencies

*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Data model representing the task to be dispatched.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Data model representing the worker node status and metadata.

## Usage Notes

*   **Implementation**: Concrete implementations (e.g., `QueueDepthScorer`, `ResourceAvailabilityScorer`) should encapsulate specific logic such as CPU load, memory availability, or queue depth.
*   **Scoring Range**: Implementations must strictly adhere to the 0-100 integer range to ensure compatibility with the dispatcher's aggregation logic.
*   **Extensibility**: New scheduling heuristics can be added by implementing this interface and registering the bean within the dispatcher's scheduling context.
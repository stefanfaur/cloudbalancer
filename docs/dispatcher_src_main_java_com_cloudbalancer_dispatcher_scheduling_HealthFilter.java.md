# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/HealthFilter.java

## Overview

The `HealthFilter` class is a component of the `com.cloudbalancer.dispatcher.scheduling` package responsible for pruning lists of candidate worker nodes based on their operational health status. It implements the `WorkerFilter` interface to ensure that only workers capable of processing tasks are considered during the scheduling process.

## Public API

### `HealthFilter` class
The primary implementation of the health-based filtering logic.

### `filter` method
```java
public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates)
```
Filters the provided list of `WorkerRecord` objects. It retains only those workers whose `WorkerHealthState` is either `HEALTHY` or `RECOVERING`.

*   **Parameters**:
    *   `task`: The `TaskRecord` currently being scheduled (unused in this implementation).
    *   `candidates`: A `List` of `WorkerRecord` objects to be evaluated.
*   **Returns**: A filtered `List` containing only workers that are in a functional health state.

## Dependencies

*   `com.cloudbalancer.common.model.WorkerHealthState`: Used to evaluate the health status of worker nodes.
*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task context for scheduling.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker node data structure.
*   `java.util.List`: Standard Java collection used for input and output processing.
*   `com.cloudbalancer.dispatcher.scheduling.WorkerFilter`: The interface contract implemented by this class.

## Usage Notes

*   **Filtering Logic**: This filter is permissive regarding `RECOVERING` states. If a stricter scheduling policy is required (e.g., excluding recovering nodes), a different implementation or a decorator pattern should be applied.
*   **Statelessness**: The `filter` method is stateless and thread-safe, making it suitable for use in high-concurrency scheduling environments.
*   **Integration**: This class is intended to be used as part of a chain of filters within the dispatcher's scheduling pipeline. It does not perform resource validation; that should be handled by separate filters such as `ResourceSufficiencyFilter`.
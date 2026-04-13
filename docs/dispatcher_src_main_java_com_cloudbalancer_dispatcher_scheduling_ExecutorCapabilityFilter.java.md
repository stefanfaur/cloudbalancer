# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/ExecutorCapabilityFilter.java

## Overview

The `ExecutorCapabilityFilter` is a component of the `com.cloudbalancer.dispatcher.scheduling` package responsible for narrowing down a list of potential worker nodes during the task scheduling process. It ensures that only workers capable of running a specific task's required executor type are considered for assignment.

This class implements the `WorkerFilter` interface, allowing it to be used as part of a chain of filters that evaluate worker suitability based on various criteria (such as resource availability or hardware requirements).

## Public API

### `ExecutorCapabilityFilter`

*   **Constructor**: `public ExecutorCapabilityFilter()`
    *   Initializes a new instance of the filter.

### `filter`

```java
public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates)
```

*   **Description**: Filters a list of `WorkerRecord` candidates based on whether their capabilities match the executor type required by the provided `TaskRecord`.
*   **Parameters**:
    *   `task`: The `TaskRecord` containing the required executor type.
    *   `candidates`: A `List` of `WorkerRecord` objects available for scheduling.
*   **Returns**: A filtered `List` of `WorkerRecord` objects that support the task's executor type.

## Dependencies

*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Used to retrieve the required executor type for a task.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Used to inspect the capabilities of a worker node.
*   `java.util.List`: Used for handling collections of worker records.
*   `com.cloudbalancer.dispatcher.scheduling.WorkerFilter`: The interface contract implemented by this class.

## Usage Notes

*   **Integration**: This filter is typically used within a scheduling pipeline where multiple filters are applied sequentially to reduce the candidate pool.
*   **Filtering Logic**: The filter uses Java Streams to perform a predicate-based check. It calls `w.getCapabilities().supportsExecutor(task.getExecutorType())` for each candidate; if the result is `false`, the worker is excluded from the returned list.
*   **Performance**: Since this operation relies on stream processing, it is efficient for standard candidate list sizes. However, ensure that the `getCapabilities()` method on `WorkerRecord` is performant, as it is invoked for every candidate in the list.
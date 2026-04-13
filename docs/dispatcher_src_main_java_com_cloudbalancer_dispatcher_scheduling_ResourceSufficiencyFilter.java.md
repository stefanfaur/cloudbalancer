# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/ResourceSufficiencyFilter.java

## Overview

The `ResourceSufficiencyFilter` is a core scheduling component within the `com.cloudbalancer.dispatcher.scheduling` package. It implements the `WorkerFilter` interface to prune a list of candidate `WorkerRecord` objects, ensuring that only workers with sufficient hardware capacity (CPU, Memory, and Disk) are considered for a specific `TaskRecord`.

This filter acts as a hard constraint during the task placement process, preventing task assignment to workers that lack the necessary resources to execute the task successfully.

## Public API

### `ResourceSufficiencyFilter`

*   **`filter(TaskRecord task, List<WorkerRecord> candidates)`**: Filters the provided list of `WorkerRecord` candidates. It returns a new list containing only those workers whose current available resources meet or exceed the requirements defined in the `TaskRecord`. If the task has no resource requirements, all candidates are returned.

### Internal Methods

*   **`hasAvailableResources(WorkerRecord worker, ResourceProfile required)`**: A private helper method that calculates the remaining capacity of a worker by subtracting allocated resources from total capabilities. It returns `true` if the worker has enough CPU, memory, and disk space to satisfy the `required` profile.

## Dependencies

*   `com.cloudbalancer.common.model.ResourceProfile`: Used to define and compare resource requirements (CPU, Memory, Disk).
*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task being scheduled and its associated resource descriptors.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker node, its total capabilities, and currently allocated resources.
*   `java.util.List`: Used for handling collections of worker candidates.

## Usage Notes

*   **Constraint Enforcement**: This filter is intended to be used early in the scheduling pipeline to eliminate infeasible workers before more complex scoring or selection logic is applied.
*   **Null Safety**: If a `TaskRecord` does not specify a `ResourceProfile`, the filter assumes the task has no specific resource requirements and will return the entire candidate list unchanged.
*   **Resource Calculation**: The filter assumes that `WorkerRecord` maintains accurate state regarding `allocatedCpu`, `allocatedMemoryMb`, and `allocatedDiskMb`. If a worker's total capabilities are null, the filter treats the worker as having zero available resources and excludes it from the results.
*   **Integration**: This class is designed to be used in conjunction with other `WorkerFilter` implementations to form a chain of scheduling constraints.
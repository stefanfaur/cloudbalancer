# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/ConstraintFilter.java

## Overview

The `ConstraintFilter` class is a core component of the cloud balancer scheduling system. It implements the `WorkerFilter` interface to prune lists of candidate workers based on specific task-defined constraints. This ensures that tasks are only dispatched to workers that meet operational requirements, such as specific capability tags or explicit inclusion/exclusion lists.

## Public API

### `ConstraintFilter`

The class provides the following methods to facilitate worker selection:

*   **`filter(TaskRecord task, List<WorkerRecord> candidates)`**: 
    Processes a list of potential `WorkerRecord` candidates against the `TaskConstraints` defined in the provided `TaskRecord`. Returns a filtered list containing only the workers that satisfy all defined constraints. If no constraints are defined for the task, the original list is returned.

*   **`matchesConstraints(WorkerRecord worker, TaskConstraints constraints)`**: 
    A private helper method that evaluates a single worker against a set of constraints. It validates:
    *   **Required Tags**: The worker must possess all tags specified in the `requiredTags` set.
    *   **Blacklist**: The worker's ID must not be present in the `blacklistedWorkers` set.
    *   **Whitelist**: If the `whitelistedWorkers` set is non-empty, the worker's ID must be present in the set.

## Dependencies

*   `com.cloudbalancer.common.model.TaskConstraints`: Defines the constraints (tags, blacklists, whitelists) applied to tasks.
*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task being scheduled.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker node being evaluated.
*   `java.util.List`: Used for handling collections of workers.

## Usage Notes

*   **Constraint Evaluation**: The `matchesConstraints` logic uses an "AND" relationship between different constraint types. A worker must satisfy all provided non-empty constraints to be considered a match.
*   **Default Behavior**: If `TaskConstraints` are null, the filter assumes no restrictions and returns the entire candidate list.
*   **Performance**: The filter uses Java Streams to process candidates. For large clusters, ensure that `requiredTags` and worker IDs are indexed or optimized for lookup if this filter is called frequently in high-throughput scheduling loops.
*   **Implementation**: This class is intended to be used as part of a chain of filters (e.g., alongside `ResourceSufficiencyFilter`) to narrow down the pool of eligible workers before final task assignment.
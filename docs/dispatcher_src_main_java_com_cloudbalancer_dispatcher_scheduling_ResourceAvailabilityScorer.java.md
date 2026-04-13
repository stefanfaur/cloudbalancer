# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/ResourceAvailabilityScorer.java

## Overview

The `ResourceAvailabilityScorer` is a component of the `cloudbalancer` scheduling system responsible for evaluating the suitability of a `WorkerRecord` based on its current resource utilization. It implements the `WorkerScorer` interface to provide a normalized score (0-100) representing how much capacity a worker has available to handle new tasks.

The scorer calculates availability by averaging the free percentages of CPU, memory, and disk. It also incorporates a health-aware penalty mechanism that reduces the score for workers currently in a `RECOVERING` state, ensuring that unstable nodes are prioritized lower during the recovery phase.

## Public API

### `score(TaskRecord task, WorkerRecord worker)`
Calculates a suitability score for the provided worker.
- **Parameters**:
    - `task`: The `TaskRecord` being scheduled.
    - `worker`: The `WorkerRecord` being evaluated.
- **Returns**: An `int` between 0 and 100, where 100 represents maximum availability.
- **Logic**: 
    - Calculates the percentage of free CPU, memory, and disk.
    - Returns the average of these percentages, scaled to 0-100.
    - If the worker is in `WorkerHealthState.RECOVERING`, the score is multiplied by a penalty factor (0.25, 0.5, or 0.75) based on the duration of the recovery process.

### `getScorerName()`
- **Returns**: `String` - The identifier for this scorer, which is `"resourceAvailability"`.

## Dependencies

- `com.cloudbalancer.common.model.ResourceProfile`: Used to access total resource capacities.
- `com.cloudbalancer.common.model.WorkerHealthState`: Used to check if a worker is in a `RECOVERING` state.
- `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task context.
- `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker's current state and resource allocation.
- `java.time.Duration` & `java.time.Instant`: Used for calculating the duration of the recovery state to apply dynamic penalties.

## Usage Notes

- **Zero-Capacity Workers**: If a worker reports zero total resources for all categories, the scorer returns a score of `0` to prevent scheduling tasks to uninitialized or misconfigured nodes.
- **Recovery Penalty**: The recovery penalty is time-sensitive:
    - **0-20 seconds**: 75% penalty (25% of base score).
    - **20-40 seconds**: 50% penalty (50% of base score).
    - **40+ seconds**: 25% penalty (75% of base score).
- **Integration**: This class is intended to be used within a `WeightedScoringStrategy` or similar orchestration logic where multiple `WorkerScorer` implementations are aggregated to make final scheduling decisions.
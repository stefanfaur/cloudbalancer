# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/QueueDepthScorer.java

## Overview

The `QueueDepthScorer` is a component of the `cloudbalancer` scheduling system responsible for evaluating the suitability of a `WorkerRecord` based on its current workload. It implements the `WorkerScorer` interface to provide a normalized score that reflects how busy a worker is, favoring workers with fewer active tasks.

The scoring logic employs a linear decay model:
- A worker with 0 active tasks receives a maximum score of 100.
- A worker with 100 or more active tasks receives a minimum score of 0.
- Intermediate values are calculated proportionally to distribute load effectively across the worker pool.

## Public API

### `QueueDepthScorer`

*   **`int score(TaskRecord task, WorkerRecord worker)`**
    Calculates the suitability score for a given worker. The score is inversely proportional to the number of active tasks currently assigned to the worker.
    *   **Parameters**: 
        *   `task`: The `TaskRecord` currently being scheduled.
        *   `worker`: The `WorkerRecord` being evaluated.
    *   **Returns**: An integer between 0 and 100, where 100 represents the most available worker.

*   **`String getScorerName()`**
    Returns the unique identifier for this scoring strategy.
    *   **Returns**: `"queueDepth"`

## Dependencies

*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Used to represent the task being scheduled.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Used to retrieve the current workload (active task count) of the worker.

## Usage Notes

*   **Thresholds**: The constant `MAX_TASKS_FOR_ZERO_SCORE` is set to 100. If a worker reaches this threshold, they are considered saturated, and the scorer will return a score of 0 regardless of how many additional tasks are queued.
*   **Integration**: This class is intended to be used within a `WeightedScoringStrategy` or similar scheduling orchestration logic to influence task placement decisions.
*   **Performance**: The scoring calculation is a simple linear arithmetic operation, making it highly performant for high-frequency scheduling decisions.
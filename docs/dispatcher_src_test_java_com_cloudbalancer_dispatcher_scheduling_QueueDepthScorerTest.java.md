# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/QueueDepthScorerTest.java

## Overview

`QueueDepthScorerTest` is a JUnit 5 test suite designed to verify the logic of the `QueueDepthScorer` component. The scorer is responsible for calculating a suitability score for a worker based on its current queue depth (number of active tasks). The tests ensure that the scoring algorithm correctly penalizes workers with higher task loads and rewards idle workers.

## Public API

The test class does not expose a public API as it is a test suite. It validates the following behavior of `QueueDepthScorer`:

*   **`zeroActiveTasksScoresHigh`**: Verifies that a worker with no active tasks receives a maximum score of 100.
*   **`manyActiveTasksScoresLow`**: Verifies that a worker with a significant number of active tasks (e.g., 50) receives a proportionally lower score.
*   **`maxActiveTasksScoresZero`**: Verifies that a worker at maximum capacity (100 active tasks) receives a score of 0.
*   **`singleActiveTaskScoresNear100`**: Verifies that a worker with a single active task receives a score slightly below the maximum (99).

## Dependencies

*   `com.cloudbalancer.common.model.*`: Provides domain models for `ExecutorType`, `ResourceProfile`, `WorkerCapabilities`, and `WorkerHealthState`.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: The data structure representing the worker state being evaluated.
*   `org.junit.jupiter.api.Test`: JUnit 5 framework for test execution.
*   `org.assertj.core.api.Assertions`: Fluent assertion library used for validation.
*   `com.cloudbalancer.dispatcher.scheduling.FilterTestHelper`: Utility for generating mock task data.

## Usage Notes

*   **Test Helper**: The class utilizes a private helper method `workerWithActiveTasks(int count)` to generate `WorkerRecord` instances. This method simulates load by calling `allocateResources` on the `WorkerRecord` object a specified number of times.
*   **Scoring Logic**: The tests assume a linear or near-linear inverse relationship between task count and the returned score, where 0 tasks equals 100 points and 100 tasks equals 0 points.
*   **Execution**: These tests should be run as part of the standard Maven/Gradle build lifecycle to ensure scheduling logic remains consistent during refactoring.
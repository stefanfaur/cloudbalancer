# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/SchedulingPipelineTest.java

## Overview

`SchedulingPipelineTest` is a critical JUnit 5 test suite responsible for validating the orchestration logic of the `SchedulingPipeline` class. It ensures that the task scheduling mechanism correctly applies a sequence of `WorkerFilter` and `WorkerScorer` components to select the optimal worker for a given task.

**Note:** This file is a **HOTSPOT** within the `dispatcher` module, exhibiting high change frequency and complexity. It serves as the primary verification layer for scheduling algorithms, making it a high-risk area for regressions.

## Public API

The class contains the following test methods:

*   **`roundRobinRotatesThroughCandidates()`**: Verifies that the `ROUND_ROBIN` strategy correctly cycles through available workers across multiple selection calls.
*   **`resourceFitSelectsBestResourceMatch()`**: Validates that the `RESOURCE_FIT` strategy correctly identifies the worker with the most available capacity based on weighted scoring.
*   **`filteringRemovesIneligibleBeforeScoring()`**: Confirms that the pipeline correctly prunes ineligible workers (e.g., `DEAD` workers) before applying scoring logic.
*   **`emptyAfterFilteringReturnsEmpty()`**: Ensures that if all workers are filtered out due to capability mismatches (e.g., executor type), the pipeline returns an empty result.
*   **`noCandidatesReturnsEmpty()`**: Validates the edge case where no workers are provided to the pipeline.
*   **`leastConnectionsSelectsIdleWorker()`**: Verifies that the `LEAST_CONNECTIONS` strategy successfully prioritizes workers with lower queue depths or idle status.

## Dependencies

The test suite relies on the following components:

*   **JUnit 5**: For test lifecycle management and assertions.
*   **Mockito**: Used to mock `SchedulingConfigRepository` and simulate configuration states.
*   **AssertJ**: Provides fluent assertions for validating pipeline output.
*   **`FilterTestHelper`**: A utility class providing factory methods for creating mock tasks and workers (`workerRecord`, `workerWithCapacity`, `anyTask`, etc.).
*   **`SchedulingPipeline`**: The system under test (SUT).

## Usage Notes

### Implementation Rationale
The test suite utilizes a factory method, `pipelineWithStrategy(String strategyName, Map<String, Integer> weights)`, to encapsulate the boilerplate setup of the `SchedulingPipeline`. This allows for clean, readable tests that focus on specific scheduling behaviors rather than infrastructure setup.

### Testing Strategy
1.  **Isolation**: Each test initializes a fresh `SchedulingPipeline` instance with mocked repository data to ensure test independence.
2.  **Edge Case Coverage**: The suite explicitly tests empty candidate lists and scenarios where filters remove all potential candidates, preventing `NullPointerException` or unexpected behavior in production.
3.  **Weighting Validation**: The `resourceFitSelectsBestResourceMatch` test validates that the pipeline respects the `SchedulingConfigRecord` weights, which is crucial for tuning the balancer in production.

### Potential Pitfalls
*   **Mocking Configuration**: Because the pipeline depends on `SchedulingConfigService` (which reads from a repository), ensure that any changes to the `SchedulingConfigRecord` structure are reflected in the `pipelineWithStrategy` helper method.
*   **Filter/Scorer Order**: The pipeline applies filters before scorers. If a new filter is added to the `SchedulingPipeline` constructor, ensure it is also reflected in the `pipelineWithStrategy` helper to maintain parity with the production environment.
*   **Hotspot Risk**: Given the high frequency of changes to this file, always run the full suite before submitting changes to the scheduling logic to ensure that existing strategies (Round Robin, Least Connections, etc.) remain functional.
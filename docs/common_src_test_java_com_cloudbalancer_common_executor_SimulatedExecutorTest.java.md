# File: common/src/test/java/com/cloudbalancer/common/executor/SimulatedExecutorTest.java

## Overview

`SimulatedExecutorTest` is the comprehensive JUnit 5 test suite for the `SimulatedExecutor` class. This class is a **critical hotspot** within the `common` module, representing a high-risk area for bugs due to its frequent changes and central role in system testing.

The suite ensures that the `SimulatedExecutor`—which mimics task execution for development and testing environments—correctly validates input specifications, reports accurate capabilities, estimates resource requirements, and handles execution lifecycles, including success, failure, and interruption scenarios.

## Public API

The test suite validates the following core behaviors of the `SimulatedExecutor`:

*   **`validate(Map<String, Object> spec)`**: Ensures the executor rejects malformed configurations (e.g., negative durations, missing required fields, or invalid probability ranges).
*   **`getCapabilities()`**: Verifies that the executor correctly reports its `SANDBOXED` security level and lack of external dependencies (Docker/Network).
*   **`estimateResources(Map<String, Object> spec)`**: Confirms that resource estimation logic produces valid, non-zero values based on input specifications.
*   **`execute(Map<String, Object> spec, ResourceAllocation alloc, TaskContext ctx)`**: 
    *   **Timing**: Validates that the executor blocks for the configured duration.
    *   **Probabilistic Failure**: Ensures `failProbability` parameters are respected (0.0 for guaranteed success, 1.0 for guaranteed failure).
    *   **Reporting**: Verifies that the `ExecutionResult` contains accurate duration metrics.
    *   **Interrupt Handling**: Confirms that thread interruptions are correctly translated into `timedOut` execution states.

## Dependencies

The test suite relies on the following components:

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **AssertJ**: Used for fluent, readable assertions.
*   **`com.cloudbalancer.common.model`**: Provides the domain objects (`ValidationResult`, `ExecutorCapabilities`, `ResourceEstimate`, `ExecutionResult`, `ResourceAllocation`, `TaskContext`) required for the executor interface.
*   **`SimulatedExecutor`**: The target class under test.

## Usage Notes

### Testing Lifecycle
As a hotspot file, any changes to the `SimulatedExecutor` logic must be accompanied by updates to this test suite. Because this executor is used to mock infrastructure, failures here often indicate regressions in the testing harness itself rather than the production system.

### Handling Time-Sensitive Tests
The `executeSleepsForConfiguredDuration` test includes a timing slack buffer (e.g., `400ms` check for a `500ms` task). When modifying or debugging these tests:
1.  **Avoid tight assertions**: Do not assert exact millisecond equality, as CI/CD environments may introduce jitter.
2.  **Interrupt Handling**: The `executeHandlesInterruptAsTimeout` test explicitly sets `Thread.currentThread().interrupt()` to verify the executor's resilience. Ensure that any future changes to the `execute` method maintain this thread-interruption awareness.

### Example: Adding a New Validation Rule
If you add a new configuration parameter (e.g., `maxMemory`) to the `SimulatedExecutor`, follow this pattern:
1.  Add a test case to `validateRejectsInvalidMaxMemory` to ensure the validator catches out-of-bounds values.
2.  Add a test case to `validateAcceptsValidSpec` to ensure the new parameter is correctly parsed.
3.  Update `estimateResourcesBasedOnSpec` if the new parameter impacts resource calculations.
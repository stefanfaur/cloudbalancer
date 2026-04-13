# File: worker/src/test/java/com/cloudbalancer/worker/service/TaskExecutionServiceCounterTest.java

## Overview

`TaskExecutionServiceCounterTest` is a critical unit test suite designed to validate the telemetry and monitoring capabilities of the `TaskExecutionService`. It ensures that the service accurately tracks task lifecycle metrics, including completion counts, failure counts, active task concurrency, and execution duration averages.

**Note:** This file is identified as a **HOTSPOT** within the repository. It exhibits high change frequency and complexity, serving as a primary validation point for the core task execution logic. Changes to the metrics collection or concurrency handling in `TaskExecutionService` should be carefully verified against this test suite to prevent regressions in system observability.

## Public API

The test suite validates the following methods exposed by `TaskExecutionService`:

*   `getCompletedTaskCount()`: Returns the total number of successfully executed tasks.
*   `getFailedTaskCount()`: Returns the total number of tasks that failed during execution.
*   `getActiveTaskCount()`: Returns the current number of tasks being processed by the worker.
*   `getAverageExecutionDurationMs()`: Returns the moving average of execution time for completed tasks.

## Dependencies

The test suite relies on the following components:
*   **JUnit 5 & Mockito**: Used for test lifecycle management and mocking external dependencies.
*   **SimulatedExecutor**: A mock executor used to simulate task duration and failure probabilities without requiring actual infrastructure.
*   **Resilience4j CircuitBreaker**: Mocked to ensure that execution logic is triggered synchronously during testing.
*   **KafkaTemplate**: Mocked to handle event dispatching requirements of the service.
*   **AssertJ**: Used for fluent assertion syntax.

## Usage Notes

### Testing Concurrency
The `countersAreThreadSafe` test is particularly important. It uses a `FixedThreadPool` to hammer the `executeTask` method with concurrent requests. 
*   **Implementation Rationale**: The test verifies that the internal counters (likely `AtomicLong` or similar thread-safe primitives) do not suffer from race conditions under load.
*   **Edge Cases**: The test uses a `CountDownLatch` to ensure all threads complete before asserting the final state, preventing false negatives due to timing issues.

### Simulating Task Behavior
The test utilizes two helper methods to generate `TaskAssignment` objects with specific characteristics:
1.  `createAssignment(int durationMs, double failProbability)`: Creates a standard task.
2.  `createAssignmentWithTimeout(...)`: Creates a task with specific `ExecutionPolicy` settings, useful for testing timeout-related metrics.

### Example: Verifying Active Task Counts
Because `executeTask` is blocking, observing the "active" state requires running the task in a separate thread and polling the service:

```java
// Example of the polling pattern used in the test
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(() -> service.executeTask(assignment));

// Poll until the service reflects the active state
for (int i = 0; i < 50; i++) {
    if (service.getActiveTaskCount() >= 1) break;
    Thread.sleep(50);
}
assertThat(service.getActiveTaskCount()).isGreaterThan(0);
```

### Potential Pitfalls
*   **Timing Sensitivity**: The `averageExecutionDuration` test relies on `isBetween(80.0, 200.0)`. If the `SimulatedExecutor` behavior changes or if the system environment is under extreme load, this assertion might become flaky.
*   **Mock Configuration**: The `setUp` method forces the `CircuitBreaker` to execute tasks immediately. If the `CircuitBreaker` logic is updated to be asynchronous or state-dependent, these tests may require significant refactoring.
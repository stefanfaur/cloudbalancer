# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/RetryScannerTest.java

## Overview

`RetryScannerTest` is a comprehensive JUnit 5 test suite designed to validate the automated lifecycle management logic for failed tasks within the `dispatcher` module. It ensures that the `RetryScanner` correctly identifies tasks in a `FAILED` state and applies appropriate recovery or termination policies based on task configuration and failure history.

**Note:** This file is a **HOTSPOT** within the repository, ranking in the top 25% for both change frequency and complexity. As it governs the critical path for task recovery and dead-lettering, it is a high-risk area for regressions.

## Public API

The test suite validates the following core behaviors of the `RetryScanner` service:

*   **`taskWithRetriesEligibleIsRequeued`**: Verifies that tasks with remaining retry attempts and an expired backoff period are successfully transitioned back to the `QUEUED` state.
*   **`taskWithMaxRetriesIsDeadLettered`**: Ensures that tasks exceeding their `maxRetries` limit are moved to `DEAD_LETTERED` and trigger a corresponding event.
*   **`taskNotYetEligibleIsSkipped`**: Confirms that tasks within their backoff window (`retryEligibleAt` in the future) are ignored during the scan.
*   **`taskWithDeadLetterPolicyIsDeadLetteredImmediately`**: Validates that tasks configured with `FailureAction.DEAD_LETTER` are moved to the dead-letter queue immediately upon failure, regardless of retry counts.
*   **`poisonPillDetected`**: Tests the "poison pill" detection logic, where a task experiencing multiple rapid, short-lived failures across different workers is automatically dead-lettered to prevent resource waste.

### Helper Methods
*   **`createFailedTask(ExecutionPolicy)`**: A utility method that simulates the full lifecycle of a task (`SUBMITTED` → `VALIDATED` → `QUEUED` → `ASSIGNED` → `PROVISIONING` → `RUNNING` → `FAILED`), providing a standardized starting point for all test cases.

## Dependencies

The test suite relies on the following components:
*   **JUnit 5 & Mockito**: Used for test orchestration and mocking external dependencies.
*   **`TaskRepository`**: Mocked to simulate database state and task retrieval.
*   **`TaskService`**: Mocked to verify state transition updates.
*   **`EventPublisher`**: Mocked to verify that dead-letter events are correctly emitted to the message bus.
*   **`TaskRecord`**: The domain model representing the task being processed.

## Usage Notes

### Testing Lifecycle Transitions
When adding new test cases, always use the `createFailedTask` helper to ensure the `TaskRecord` is in a valid state. Manually constructing `TaskRecord` objects without following the state machine transitions may lead to false negatives or unexpected `IllegalStateException` errors in the `TaskService`.

### Poison Pill Logic
The `poisonPillDetected` test case is particularly sensitive. It relies on the `RetryScanner` configuration parameters (specifically the `5` attempts and `2000ms` duration threshold defined in `setUp`). If the `RetryScanner` constructor signature or default thresholds change, this test must be updated to reflect the new sensitivity of the poison pill detection.

### Mocking Strategy
The tests use `ArgumentMatchers` (`argThat`) to inspect the state of `TaskRecord` objects passed to `taskService.updateTask()`. When debugging, ensure that the `TaskRecord` state transitions are fully captured by the `verify` blocks, as the `RetryScanner` modifies the task object in-place before passing it to the service.

### Example: Adding a New Test Case
To test a new failure scenario (e.g., a specific error code), follow this pattern:
1.  Use `createFailedTask` to generate the base record.
2.  Inject the specific `ExecutionAttempt` or `TaskDescriptor` properties required for the scenario.
3.  Mock `taskRepository.findByStateIn` to return your custom task.
4.  Invoke `retryScanner.scanAndRetry()`.
5.  Use `verify(taskService).updateTask(...)` to assert the expected `TaskState`.
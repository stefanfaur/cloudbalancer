# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/TaskRecordRepositoryTest.java

## Overview

`TaskRecordRepositoryTest` is an integration test suite for the `TaskRepository` component within the `dispatcher` module. It validates the persistence layer's ability to store, retrieve, and query `TaskRecord` entities, specifically focusing on JSONB serialization of task descriptors and state-based filtering. The tests utilize a containerized database environment to ensure consistency with production configurations.

## Public API

The class provides the following test methods:

*   **`persistAndRetrieveWithJsonb`**: Verifies that a `TaskRecord` can be persisted to the database and retrieved with its complex `TaskDescriptor` (including JSONB fields) intact.
*   **`queryByState`**: Validates that tasks can be filtered correctly based on their current `TaskState`.
*   **`queryByStateOrderedByPriorityAndTime`**: Ensures that retrieved tasks can be sorted by their priority and submission timestamp.

## Dependencies

*   **`com.cloudbalancer.common.model`**: Provides core domain models (`TaskRecord`, `TaskDescriptor`, `TaskState`, `Priority`, etc.).
*   **`com.cloudbalancer.dispatcher.test.TestContainersConfig`**: Provides the infrastructure for running tests against a containerized database.
*   **`org.springframework.boot.test.context.SpringBootTest`**: Used to load the application context for integration testing.
*   **`org.assertj.core.api.Assertions`**: Used for fluent assertion checks.

## Usage Notes

*   **Environment**: This test requires a running database container as configured in `TestContainersConfig`. Ensure Docker is available in the environment where tests are executed.
*   **Cleanup**: The `@BeforeEach` annotated `cleanUp` method ensures that the `TaskRepository` is cleared before every test execution, providing a clean state and preventing test interference.
*   **Helper Methods**: The `saveTaskInState` private method is used to facilitate test setup by automating the state machine transitions required to place a task into a specific `TaskState` before saving it to the repository.
*   **Sorting Logic**: Note that `queryByStateOrderedByPriorityAndTime` performs manual sorting in the test code using Java's `Comparator` to verify that the repository data can be correctly ordered by the application layer based on `Priority` ordinal values and submission time.
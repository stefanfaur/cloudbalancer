# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/TaskTimestampsTest.java

## Overview

`TaskTimestampsTest` is a critical integration test suite for the `dispatcher` module. It validates the persistence layer's ability to track the lifecycle of a task via specific temporal metadata: `assigned_at`, `started_at`, and `completed_at`.

**Warning: Hotspot File**
This file is identified as a **hotspot** (top 25% for change frequency and complexity). It is a high-risk area for bugs, as changes to the database schema or task state transition logic often require corresponding updates here. Ensure that any modifications to the `TaskRecord` entity or the underlying SQL migration scripts are verified against these tests.

## Public API

The class provides the following test cases to validate the persistence contract:

*   **`v2MigrationAddsTimestampColumns()`**: Verifies that the database schema migration correctly initialized the `assigned_at`, `started_at`, and `completed_at` columns in the `tasks` table.
*   **`assignedAtSetOnAssignment()`**: Ensures that when a task transitions to the `ASSIGNED` state, the `assigned_at` timestamp is correctly persisted and retrieved.
*   **`startedAtAndCompletedAtSetOnResultProcessing()`**: Validates that `started_at` and `completed_at` timestamps are correctly stored when a task completes its execution cycle.
*   **`timestampsNullableForUnassignedTasks()`**: Confirms that for tasks in early lifecycle stages (e.g., `QUEUED`), these timestamp fields remain `null`, preventing premature or incorrect data population.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Spring Boot Test**: Provides the application context and dependency injection for `TaskRepository` and `JdbcTemplate`.
*   **TestContainers**: Managed via `TestContainersConfig` to provide a real database environment for integration testing.
*   **AssertJ**: Used for fluent, readable assertions.
*   **`com.cloudbalancer.common.model`**: Provides the domain models (`TaskRecord`, `TaskState`, `TaskDescriptor`, etc.) used to simulate task lifecycles.

## Usage Notes

### Implementation Rationale
The tests utilize `JdbcTemplate` to perform raw SQL queries against the `information_schema` to verify schema integrity, while using the `TaskRepository` to verify the ORM mapping and state persistence. This dual-layer approach ensures that both the database schema and the application's data access logic are synchronized.

### Testing Lifecycle
1.  **Setup**: The `cleanUp()` method runs before every test to ensure a deterministic state by truncating the `tasks` table.
2.  **Helper Methods**: The `createTask()` method acts as a factory for generating valid `TaskRecord` objects with default constraints, ensuring consistency across test cases.

### Common Pitfalls
*   **State Transitions**: Tests rely on the `TaskRecord.transitionTo()` method. If the state machine logic in `TaskRecord` changes, these tests may fail even if the persistence logic remains correct.
*   **Time Sensitivity**: `assignedAtSetOnAssignment` uses `Duration.between` to verify timestamps. If the test environment is under heavy load, the 5-second threshold might be exceeded, leading to flaky tests.
*   **Migration Dependencies**: If the `v2` migration is renamed or refactored, `v2MigrationAddsTimestampColumns` will fail. Always verify that the migration script name matches the expectation in the test.

### Example: Simulating a Task Lifecycle
To manually test a task lifecycle in a similar environment, follow this pattern:
```java
// 1. Create and persist
TaskRecord record = createTask();
taskRepository.save(record);

// 2. Transition and update
record.transitionTo(TaskState.ASSIGNED);
record.setAssignedAt(Instant.now());
taskRepository.save(record);

// 3. Verify
TaskRecord reloaded = taskRepository.findById(record.getId()).get();
assertNotNull(reloaded.getAssignedAt());
```
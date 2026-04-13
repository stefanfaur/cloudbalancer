# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/SchedulingConfigRepositoryTest.java

## Overview

`SchedulingConfigRepositoryTest` is a Spring Boot integration test suite designed to validate the persistence operations of the `SchedulingConfigRepository`. It ensures that scheduling configurations, including strategy names and associated weight maps, are correctly persisted, retrieved, and updated in the database.

The test suite utilizes `TestContainersConfig` to provide a containerized database environment, ensuring consistent and isolated testing of repository operations.

## Public API

### `SchedulingConfigRepositoryTest`

*   **`cleanUp()`**: Executed before each test case to ensure a clean database state by invoking `schedulingConfigRepository.deleteAll()`.
*   **`persistAndRetrieveWithWeights()`**: Verifies that a `SchedulingConfigRecord` containing a strategy name and a map of weights can be successfully saved and retrieved from the repository.
*   **`updateStrategyConfig()`**: Validates the repository's ability to update an existing `SchedulingConfigRecord`, ensuring that changes to the strategy name and weight map are correctly persisted.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Spring Boot Test**: Provides the testing context and dependency injection for the repository.
*   **AssertJ**: Used for fluent assertions on repository results.
*   **TestContainers**: Configured via `TestContainersConfig` to provide a real database instance for integration testing.
*   **SchedulingConfigRepository**: The target component under test.
*   **SchedulingConfigRecord**: The data model being persisted.

## Usage Notes

*   **Environment**: This test requires a running Docker environment due to the `TestContainersConfig` dependency.
*   **Database State**: The `cleanUp` method ensures that tests are idempotent by clearing the repository before every test execution.
*   **Integration**: These tests verify the full stack from the repository interface down to the database, confirming that JPA mappings and database constraints are correctly configured for `SchedulingConfigRecord`.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/TaskServiceJpaTest.java

## Overview

`TaskServiceJpaTest` is an integration test suite designed to verify the persistence layer and service logic of the `TaskService` within the `dispatcher` module. It ensures that task lifecycle operations—such as submission, retrieval, and listing—correctly interact with the underlying database using JPA.

The suite utilizes Spring Boot's testing infrastructure and `TestContainersConfig` to provide a real database environment, ensuring that repository queries and transaction boundaries function as expected in a production-like setting.

## Public API

The class is a test suite and does not expose a public API for production use. It contains the following test methods:

*   **`cleanUp()`**: A `@BeforeEach` hook that clears the `TaskRepository` before every test case to ensure test isolation.
*   **`submitTaskPersistsToDatabase()`**: Verifies that submitting a task via `TaskService` correctly persists the entity to the database with a `QUEUED` state.
*   **`getTaskReturnsEnvelope()`**: Validates that a previously submitted task can be retrieved by its ID and that the returned data matches the original descriptor.
*   **`getQueuedTasksOrderedByPriority()`**: Confirms that the service correctly retrieves queued tasks sorted by their `Priority` (CRITICAL > NORMAL > LOW).
*   **`listTasksReturnsAll()`**: Ensures that the `listTasks()` method returns the total count of tasks currently persisted in the system.

## Dependencies

*   **`com.cloudbalancer.common.model.*`**: Provides core domain models like `TaskDescriptor`, `Priority`, `TaskState`, and `ResourceProfile`.
*   **`com.cloudbalancer.dispatcher.persistence.TaskRepository`**: The JPA repository interface used for database interactions.
*   **`com.cloudbalancer.dispatcher.test.TestContainersConfig`**: Configuration class providing containerized database support for integration testing.
*   **Spring Boot Test**: Utilizes `@SpringBootTest` for full application context loading and `@Autowired` for dependency injection.
*   **AssertJ**: Used for fluent assertion syntax in test validations.

## Usage Notes

*   **Test Environment**: This test requires a running Docker environment (via Testcontainers) to initialize the database. Ensure the environment is configured to support containerized services.
*   **Data Isolation**: Every test case starts with an empty database state due to the `@BeforeEach` `cleanUp` method. Do not rely on data persistence across different test methods.
*   **Helper Methods**: The `testDescriptor(Priority priority)` method is a private utility used to generate standardized `TaskDescriptor` objects for testing various priority scenarios without duplicating setup code.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/security/UserRepositoryTest.java

## Overview

`UserRepositoryTest` is a Spring Boot integration test suite designed to validate the persistence layer of the `UserRepository`. It ensures that user entities can be correctly saved to and retrieved from the database, specifically verifying the custom query method `findByUsername`.

The test suite utilizes `TestContainersConfig` to provide a containerized database environment, ensuring tests are isolated and consistent with production-like infrastructure.

## Public API

### `UserRepositoryTest` (Class)
The main test class annotated with `@SpringBootTest` and `@Import(TestContainersConfig.class)`. It manages the lifecycle of the `UserRepository` bean for integration testing.

### `saveAndFindByUsername()` (Method)
Verifies the full lifecycle of a `User` entity:
- Persists a new `User` object with defined attributes (username, password, role, enabled status).
- Retrieves the user by username using `userRepository.findByUsername()`.
- Asserts that the retrieved entity matches the persisted data.

### `findByUsernameReturnsEmptyForMissing()` (Method)
Verifies the negative case for data retrieval:
- Attempts to query a username that does not exist in the database.
- Asserts that the returned `Optional` is empty.

## Dependencies

- **JUnit 5**: Used for test execution and lifecycle management.
- **Spring Boot Test**: Provides the testing context and dependency injection for `UserRepository`.
- **TestContainers**: Used via `TestContainersConfig` to spin up a transient database instance for integration testing.
- **AssertJ**: Used for fluent assertions on test results.
- **`com.cloudbalancer.common.model.Role`**: Domain model dependency for user roles.

## Usage Notes

- **Environment Requirements**: This test requires a Docker-compatible environment to run the `TestContainers` instance. Ensure Docker is running on the host machine during test execution.
- **Database State**: As an integration test, this class interacts with a real database. The tests are designed to be self-contained; however, ensure that the database schema is correctly initialized by the Spring Boot context before the tests execute.
- **Execution**: Run these tests using standard Maven or Gradle commands (e.g., `mvn test -Dtest=UserRepositoryTest`).
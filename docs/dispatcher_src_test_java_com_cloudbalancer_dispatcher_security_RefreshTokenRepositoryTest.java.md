# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/security/RefreshTokenRepositoryTest.java

## Overview

`RefreshTokenRepositoryTest` is an integration test suite for the `RefreshTokenRepository` component within the `dispatcher` module. It validates the persistence layer's ability to store, retrieve, and handle missing refresh tokens in the database. The tests utilize Spring Boot's testing infrastructure and `TestContainersConfig` to ensure a consistent, isolated database environment during execution.

## Public API

### `RefreshTokenRepositoryTest`
The test class is annotated with `@SpringBootTest`, `@Import(TestContainersConfig.class)`, and `@Transactional`. It performs full context loading to verify the interaction between the `RefreshTokenRepository` and the underlying database.

### Methods

- **`saveAndFindByToken()`**: Verifies that a `RefreshToken` entity can be successfully persisted to the database and subsequently retrieved using the unique token string. It asserts that the associated `User` data and token properties (such as revocation status) remain consistent after retrieval.
- **`findByTokenReturnsEmptyForMissing()`**: Verifies that querying the repository for a token string that does not exist in the database correctly returns an empty `Optional`, ensuring robust error handling for invalid or expired tokens.

## Dependencies

- **`RefreshTokenRepository`**: The primary repository under test.
- **`UserRepository`**: Used to create and persist a valid `User` entity required for foreign key constraints when saving a `RefreshToken`.
- **`TestContainersConfig`**: Provides the containerized database environment for integration testing.
- **`Spring Boot Test`**: Provides the testing framework and dependency injection context.
- **`AssertJ`**: Used for fluent assertion syntax in test verification.

## Usage Notes

- **Transactional Integrity**: The class is marked with `@Transactional`, meaning each test method runs within a transaction that is rolled back upon completion. This ensures that test data does not persist between test runs.
- **Prerequisites**: Ensure that the Docker daemon is running, as `TestContainersConfig` requires it to spin up the database instance for integration tests.
- **Data Setup**: The `saveAndFindByToken` test dynamically generates a unique username using `UUID` to avoid conflicts with existing database records.
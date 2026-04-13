# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/security/UserServiceTest.java

## Overview

`UserServiceTest` is an integration test suite for the `UserService` component within the `dispatcher` module. It validates core security functionalities, including user authentication workflows and password management. The tests ensure that the system correctly handles valid credentials, rejects invalid attempts, and enforces secure password storage using hashing mechanisms.

## Public API

The class contains the following test methods:

*   **`authenticateWithValidCredentials()`**: Verifies that a user created with specific credentials can be successfully authenticated and that the returned user object contains the correct role.
*   **`authenticateWithWrongPasswordReturnsEmpty()`**: Ensures that authentication fails (returns an empty `Optional`) when the provided password does not match the stored hash.
*   **`authenticateWithNonexistentUserReturnsEmpty()`**: Confirms that authentication attempts for non-existent usernames return an empty `Optional`.
*   **`createUserHashesPassword()`**: Validates that `UserService` correctly hashes passwords during user creation, ensuring the raw password is not stored in plain text in the repository.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Spring Boot Test**: Provides the `@SpringBootTest` context for full application integration testing.
*   **TestContainers**: Imported via `TestContainersConfig` to provide a containerized database environment for isolated testing.
*   **AssertJ**: Used for fluent, readable assertions.
*   **Spring Security (BCryptPasswordEncoder)**: Used to verify that stored passwords are correctly hashed.
*   **Internal Components**:
    *   `UserService`: The service under test.
    *   `UserRepository`: Used to verify the state of the database directly.
    *   `Role`: Enum defining user authorization levels.

## Usage Notes

*   **Environment Requirements**: This test suite requires a running containerized database (via `TestContainersConfig`). Ensure Docker is running in the environment where these tests are executed.
*   **Data Isolation**: Each test generates a unique username using `UUID.randomUUID()` to prevent collisions and ensure test isolation within the shared database.
*   **Security Verification**: The `createUserHashesPassword` test specifically checks the persistence layer to ensure that the `UserService` is correctly invoking the password encoder before saving to the database.
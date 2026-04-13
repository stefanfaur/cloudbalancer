# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/registration/AgentTokenServiceTest.java

## Overview

`AgentTokenServiceTest` is a comprehensive integration test suite for the `AgentTokenService` class within the `dispatcher` module. It validates the lifecycle of agent authentication tokens, including generation, secure storage (hashing), validation, revocation, and audit tracking (last-used timestamps).

The test suite utilizes Spring Boot's testing infrastructure and `TestContainersConfig` to ensure that token operations are verified against a real database environment, maintaining consistency with production-like persistence logic.

## Public API

The test class does not expose a public API as it is a test suite. However, it exercises the following methods of the `AgentTokenService`:

*   **`create(String label, String owner)`**: Verifies token generation, prefixing (`cb_at_`), and SHA-256 hash storage.
*   **`validate(String token)`**: Ensures correct identification of valid tokens, rejection of invalid/bogus tokens, and rejection of revoked tokens.
*   **`revoke(UUID id)`**: Confirms that the revocation flag is correctly persisted in the repository.

## Dependencies

*   **JUnit 5**: The testing framework used for test execution.
*   **Spring Boot Test**: Provides the `@SpringBootTest` context for dependency injection.
*   **AssertJ**: Used for fluent assertions (e.g., `assertThat`).
*   **TestContainersConfig**: A custom configuration class providing containerized infrastructure for integration testing.
*   **AgentTokenService**: The service under test.
*   **AgentTokenRepository**: Used to verify the side effects of service operations on the underlying data store.

## Usage Notes

*   **Integration Testing**: This test requires a running database instance provided by `TestContainersConfig`. Ensure that Docker is available in the environment where these tests are executed.
*   **Security Verification**: The tests explicitly verify that raw tokens are not stored in plain text by comparing the stored hash against the result of `AgentTokenService.sha256(token)`.
*   **Side Effects**: Tests verify that `validate()` is not just a read-only operation but also updates the `lastUsedAt` field, ensuring that agent activity is correctly audited.
*   **Execution**: Run these tests via standard Maven or Gradle commands (e.g., `mvn test -Dtest=AgentTokenServiceTest`).
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/security/RateLimitFilterTest.java

## Overview

`RateLimitFilterTest` is a Spring Boot integration test suite designed to validate the security rate-limiting mechanisms within the `dispatcher` module. It ensures that incoming requests are correctly throttled based on user roles and authentication status, preventing abuse of the API.

The test suite utilizes `MockMvc` to simulate HTTP requests and `TestContainersConfig` to provide a consistent environment for verifying that the rate-limiting logic correctly enforces defined thresholds and returns appropriate HTTP status codes (e.g., `429 Too Many Requests`).

## Public API

The class provides the following test methods:

- `properties(DynamicPropertyRegistry registry)`: Configures the test environment with low, deterministic rate-limit thresholds for `VIEWER` and `ADMIN` roles.
- `exceedingRateLimitReturns429()`: Verifies that once a user exceeds their assigned request quota, the system returns a `429 Too Many Requests` status and includes a `Retry-After` header.
- `differentUsersHaveIndependentLimits()`: Confirms that rate-limiting buckets are isolated per user, ensuring one user's activity does not impact another's quota.
- `unauthenticatedRequestsAreRateLimited()`: Validates that requests without valid authentication tokens are handled correctly by the security filter chain without causing system instability.

## Dependencies

- **JUnit 5**: Used as the primary testing framework.
- **Spring Boot Test**: Provides the `@SpringBootTest` and `@AutoConfigureMockMvc` annotations for full application context testing.
- **MockMvc**: Used for performing simulated HTTP requests against the controller layer.
- **TestContainers**: Used via `TestContainersConfig` to manage infrastructure dependencies required for the security filter.
- **JwtService**: Injected to generate valid authentication tokens for test scenarios.

## Usage Notes

- **Environment Configuration**: The test overrides default rate-limit properties using `@DynamicPropertySource`. These values (`3` for `VIEWER`, `5` for `ADMIN`) are intentionally low to facilitate rapid testing of threshold exhaustion.
- **Isolation**: Each test case is designed to be independent. The `differentUsersHaveIndependentLimits` test specifically ensures that the underlying rate-limiting storage (e.g., Redis or in-memory map) correctly keys limits by user identifier.
- **Execution**: As an integration test, it requires a running Spring context. Ensure that the `TestContainers` environment is correctly configured in your local development environment to avoid connection timeouts during test execution.
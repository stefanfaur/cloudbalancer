# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/security/MetricsSecurityTest.java

## Overview

`MetricsSecurityTest` is a Spring Boot integration test class located in the `metrics-aggregator` module. It is designed to verify the security configuration and JWT-based authentication mechanisms for the metrics API endpoints. By utilizing `MockMvc`, the test suite ensures that unauthorized access is correctly blocked and that valid, expired, or malformed tokens are handled according to the system's security policy.

## Public API

The class provides the following test methods:

*   **`unauthenticatedRequestReturns401()`**: Verifies that requests made without an `Authorization` header result in an HTTP 401 Unauthorized status.
*   **`validJwtReturns200()`**: Verifies that requests containing a valid, non-expired JWT with appropriate roles result in an HTTP 200 OK status.
*   **`expiredJwtReturns401()`**: Verifies that requests containing a JWT that has passed its expiration time result in an HTTP 401 Unauthorized status.
*   **`invalidJwtReturns401()`**: Verifies that requests containing malformed or otherwise invalid JWT strings result in an HTTP 401 Unauthorized status.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Spring Boot Test**: Provides the testing infrastructure, including `SpringBootTest` and `AutoConfigureMockMvc`.
*   **MockMvc**: Used to perform simulated HTTP requests against the application context.
*   **TestContainersConfig**: An imported configuration class used to manage containerized dependencies required for the test environment.
*   **JwtService**: An internal service used to generate valid, expired, and test-specific tokens.
*   **com.cloudbalancer.common.model.Role**: Used to define authorization roles during token generation.

## Usage Notes

*   **Environment**: This test requires a running Spring context. It uses `SpringBootTest.WebEnvironment.RANDOM_PORT` to avoid port conflicts during parallel test execution.
*   **Configuration**: The test relies on `TestContainersConfig`, implying that a database or other infrastructure component (likely managed by Testcontainers) must be available in the environment where these tests are executed.
*   **Integration**: These tests target the `/api/metrics/workers` endpoint. Ensure that the security filter chain is correctly configured in the main application to intercept these paths before running the tests.
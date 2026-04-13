# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/api/CorsTest.java

## Overview

`CorsTest` is a specialized integration test suite designed to verify the Cross-Origin Resource Sharing (CORS) configuration of the `metrics-aggregator` API. It ensures that the application correctly handles preflight requests by validating allowed origins and rejecting unauthorized ones, thereby securing the API against unauthorized cross-origin access.

## Public API

### `CorsTest`
The test class utilizes Spring Boot's `MockMvc` to simulate HTTP requests against the application context.

*   **`preflightFromAllowedOriginReturnsCorsHeaders()`**: Validates that a preflight `OPTIONS` request originating from a trusted source (e.g., `http://localhost:5173`) receives the expected CORS headers (`Access-Control-Allow-Origin` and `Access-Control-Allow-Credentials`).
*   **`preflightFromUnknownOriginDoesNotReturnCorsHeaders()`**: Validates that a preflight `OPTIONS` request from an untrusted or unknown origin (e.g., `http://evil.com`) does not receive CORS headers, effectively blocking the cross-origin request.

## Dependencies

*   **JUnit 5**: Used for test execution and lifecycle management.
*   **Spring Boot Test**: Provides the `SpringBootTest` and `AutoConfigureMockMvc` annotations to load the application context and mock the web layer.
*   **MockMvc**: Used for performing simulated HTTP requests and asserting response outcomes.
*   **TestContainersConfig**: Imported to provide necessary containerized infrastructure dependencies required for the test environment.

## Usage Notes

*   **Environment**: This test requires a full Spring Boot application context. Ensure that the `TestContainersConfig` is correctly configured to provide any required backing services (e.g., databases or message brokers) before running the suite.
*   **CORS Policy**: The tests are hardcoded to expect specific behavior based on the current CORS configuration in the application. If the allowed origins list in the application's security configuration changes, these tests must be updated to reflect the new policy.
*   **Execution**: Run these tests as part of the standard Maven/Gradle build lifecycle to ensure that security regressions regarding cross-origin access are caught during CI/CD.
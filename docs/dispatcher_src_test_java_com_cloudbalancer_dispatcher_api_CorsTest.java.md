# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/CorsTest.java

## Overview

`CorsTest` is an integration test suite for the `dispatcher` service, specifically designed to validate Cross-Origin Resource Sharing (CORS) security policies. The test suite ensures that the application correctly handles preflight `OPTIONS` requests by verifying that allowed origins receive the appropriate CORS headers, while unauthorized origins are restricted.

## Public API

### `CorsTest`
The primary test class that utilizes Spring Boot's `MockMvc` to simulate HTTP requests against the dispatcher API endpoints.

*   **`preflightFromAllowedOriginReturnsCorsHeaders()`**: Verifies that requests originating from a trusted source (e.g., `http://localhost:5173`) receive the expected `Access-Control-Allow-Origin` and `Access-Control-Allow-Credentials` headers.
*   **`preflightFromUnknownOriginDoesNotReturnCorsHeaders()`**: Verifies that requests originating from an untrusted or unknown source do not receive CORS headers, effectively blocking cross-origin access.

## Dependencies

*   **JUnit 5**: Used for test execution and lifecycle management.
*   **Spring Boot Test**: Provides the `@SpringBootTest` context and `@AutoConfigureMockMvc` for testing the web layer without starting a full HTTP server.
*   **MockMvc**: The core utility for performing simulated HTTP requests and asserting responses.
*   **TestContainersConfig**: An imported configuration class used to manage containerized dependencies required for the integration test environment.

## Usage Notes

*   **Test Context**: This class requires a full Spring application context to run. Ensure that the `TestContainersConfig` is correctly configured to provide necessary infrastructure (e.g., databases or message brokers) before running these tests.
*   **CORS Policy**: The tests assume a specific whitelist of origins. If the CORS policy is updated in the main application configuration, these tests should be updated to reflect the new allowed origins.
*   **Execution**: These tests are intended to run as part of the standard Maven/Gradle build lifecycle. They are categorized as integration tests and may take longer to execute than unit tests due to the startup of the Spring context.
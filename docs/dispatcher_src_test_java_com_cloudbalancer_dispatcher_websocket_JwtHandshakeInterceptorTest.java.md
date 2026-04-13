# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/websocket/JwtHandshakeInterceptorTest.java

## Overview

`JwtHandshakeInterceptorTest` is a unit test suite for the `JwtHandshakeInterceptor` class. Its primary purpose is to verify the security logic applied during the WebSocket handshake process, ensuring that only requests with valid JWT tokens are permitted to establish a connection. The test suite uses Mockito to mock the `JwtService` and Spring's `ServerHttpRequest`/`ServerHttpResponse` objects to simulate various handshake scenarios.

## Public API

The test class validates the `beforeHandshake` method of the `JwtHandshakeInterceptor` through the following test cases:

*   **`validTokenAllowsHandshake`**: Verifies that a request containing a valid JWT token returns `true` and successfully populates the session attributes with the extracted username.
*   **`missingTokenRejectsHandshake`**: Ensures that requests lacking a `token` query parameter are rejected with an `HttpStatus.UNAUTHORIZED` status.
*   **`blankTokenRejectsHandshake`**: Confirms that empty or blank tokens result in a rejection and an `UNAUTHORIZED` status.
*   **`invalidTokenRejectsHandshake`**: Validates that tokens failing validation via `JwtService` are rejected and do not populate session attributes.
*   **`exceptionDuringValidationRejectsHandshake`**: Tests the resilience of the interceptor by ensuring that unexpected exceptions during token validation result in a graceful rejection of the handshake.

## Dependencies

*   **JUnit 5 (Jupiter)**: Framework used for test execution and lifecycle management.
*   **Mockito**: Used for mocking `JwtService`, `ServerHttpRequest`, `ServerHttpResponse`, and `WebSocketHandler`.
*   **AssertJ**: Used for fluent assertions to verify test outcomes.
*   **Spring Framework (Web/WebSocket)**: Provides the core interfaces for HTTP server requests and responses used in the handshake process.
*   **`com.cloudbalancer.dispatcher.security.JwtService`**: The service being tested against for token validation and username extraction.

## Usage Notes

*   **Mocking Strategy**: The `setUp` method initializes a fresh mock environment before every test case to ensure test isolation.
*   **Integration**: This test specifically targets the `beforeHandshake` logic. It does not perform full integration testing of the WebSocket connection but focuses on the security interceptor's decision-making process.
*   **Attributes**: Note that when a valid token is provided, the interceptor is expected to store the `username` in the `attributes` map; tests verify this behavior explicitly to ensure downstream components have access to the authenticated user's identity.
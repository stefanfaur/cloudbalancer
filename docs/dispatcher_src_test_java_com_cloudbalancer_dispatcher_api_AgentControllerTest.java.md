# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AgentControllerTest.java

## Overview

`AgentControllerTest` is a Spring Boot integration test suite designed to validate the security and functional constraints of the `AgentController` API endpoints. It ensures that the dispatcher correctly enforces role-based access control (RBAC) and handles standard HTTP status codes for agent management operations.

The test suite utilizes `MockMvc` to simulate HTTP requests and `TestContainersConfig` to provide a containerized environment, ensuring that the controller interacts correctly with the underlying infrastructure.

## Public API

The test class does not expose a public API, as it is a test suite. However, it validates the following controller endpoints:

*   **`GET /api/admin/agents`**: Retrieves the list of registered agents.
*   **`GET /api/admin/agents/{id}`**: Retrieves details for a specific agent.

### Test Methods

*   **`listAgents_empty_returns200()`**: Verifies that an authenticated admin can successfully retrieve an empty list of agents.
*   **`getAgent_notFound_returns404()`**: Verifies that requesting a non-existent agent ID returns a 404 Not Found status.
*   **`listAgents_unauthenticated_returns401()`**: Verifies that requests without a valid JWT token are rejected with a 401 Unauthorized status.
*   **`listAgents_asOperator_returns403()`**: Verifies that users with the `OPERATOR` role are forbidden from accessing admin-only agent endpoints (403 Forbidden).

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Spring Boot Test**: Provides the testing context and `MockMvc` for web layer testing.
*   **`JwtService`**: Used to generate valid authentication tokens for `ADMIN` and `OPERATOR` roles during test execution.
*   **`TestContainersConfig`**: Configures the necessary containerized dependencies for the integration test environment.

## Usage Notes

*   **Authentication**: The test suite relies on the `JwtService` to generate tokens. To add new test cases, use the private `adminToken()` or `operatorToken()` helper methods to simulate different user roles.
*   **Environment**: This test requires a running container environment as defined in `TestContainersConfig`. Ensure that Docker is available in the environment where these tests are executed.
*   **MockMvc**: All tests use `MockMvc` to perform requests. When adding new tests for the `AgentController`, ensure the `MockMvc` instance is used to maintain consistency with the existing security integration tests.
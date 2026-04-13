# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AgentTokenControllerTest.java

## Overview

`AgentTokenControllerTest` is an integration test suite for the `AgentTokenController` in the `dispatcher` module. It validates the REST API endpoints responsible for managing agent authentication tokens, ensuring that security constraints, authorization roles, and token lifecycle operations (create, list, revoke) function as expected.

The test suite utilizes `MockMvc` to simulate HTTP requests and verifies responses against the expected API contract, ensuring that only users with the `ADMIN` role can perform administrative token operations.

## Public API

The test class covers the following API endpoints:

*   **POST `/api/admin/agent-tokens`**: Creates a new agent token.
*   **GET `/api/admin/agent-tokens`**: Lists all existing agent tokens.
*   **POST `/api/admin/agent-tokens/{id}/revoke`**: Revokes a specific agent token by ID.

### Test Methods

*   `createToken_asAdmin_returns200WithToken()`: Verifies that an authenticated admin can successfully create a token.
*   `listTokens_asAdmin_returnsCreatedToken()`: Verifies that an authenticated admin can retrieve a list of tokens.
*   `revokeToken_asAdmin_returns204()`: Verifies that an authenticated admin can revoke a token, resulting in a 204 No Content status.
*   `createToken_asOperator_returns403()`: Verifies that users with the `OPERATOR` role are forbidden from accessing admin token endpoints.
*   `listTokens_unauthenticated_returns401()`: Verifies that unauthenticated requests are rejected with a 401 Unauthorized status.

## Dependencies

*   **Spring Boot Test**: Provides the testing framework and `MockMvc` for web layer testing.
*   **TestContainersConfig**: Used to provide necessary infrastructure (e.g., databases) for integration testing.
*   **JwtService**: Used to generate valid JWT tokens for `ADMIN` and `OPERATOR` roles during test execution.
*   **AgentTokenService**: Used to perform setup operations (e.g., pre-populating tokens) before executing controller tests.
*   **JUnit 5**: The underlying testing framework.

## Usage Notes

*   **Authentication**: The tests rely on the `adminToken()` and `operatorToken()` helper methods to inject valid `Authorization` headers into the `MockMvc` requests.
*   **Environment**: This test requires a running application context and database connectivity, as configured by `TestContainersConfig`.
*   **Security Enforcement**: The tests explicitly verify that the API enforces role-based access control (RBAC), ensuring that the `OPERATOR` role cannot perform administrative actions, even if they possess a valid JWT.
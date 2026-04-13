# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AgentTokenController.java

## Overview

The `AgentTokenController` is a Spring `@RestController` responsible for managing the lifecycle of agent authentication tokens within the `dispatcher` module. It provides administrative endpoints to create, list, and revoke tokens used by cloud agents to authenticate with the system.

## Public API

### `AgentTokenController(AgentTokenService tokenService)`
Constructs the controller with the required `AgentTokenService` dependency.

### `create(CreateAgentTokenRequest request, Principal principal)`
*   **Endpoint**: `POST /api/admin/agent-tokens`
*   **Description**: Generates a new authentication token for an agent.
*   **Parameters**: 
    *   `request`: A `CreateAgentTokenRequest` containing the token label.
    *   `principal`: The authenticated user initiating the request.
*   **Returns**: A `ResponseEntity` containing a `CreateAgentTokenResponse` with the token ID, the raw token string, and the label.

### `list()`
*   **Endpoint**: `GET /api/admin/agent-tokens`
*   **Description**: Retrieves a summary list of all agent tokens currently registered in the system.
*   **Returns**: A `List<AgentTokenSummary>` containing metadata such as ID, label, creator, creation date, last usage timestamp, and revocation status.

### `revoke(UUID id)`
*   **Endpoint**: `POST /api/admin/agent-tokens/{id}/revoke`
*   **Description**: Invalidates an existing agent token by its unique identifier.
*   **Parameters**: 
    *   `id`: The `UUID` of the token to be revoked.
*   **Returns**: A `ResponseEntity` with a `204 No Content` status upon successful revocation.

## Dependencies

*   **`com.cloudbalancer.dispatcher.registration.AgentTokenService`**: The service layer component that handles the business logic for token persistence and lifecycle management.
*   **`com.cloudbalancer.dispatcher.api.dto.*`**: Data Transfer Objects used for request and response serialization.
*   **Spring Web MVC**: Provides the `@RestController`, `@RequestMapping`, and related annotations for handling HTTP requests.

## Usage Notes

*   **Authentication**: This controller is mapped under `/api/admin/`, implying that access should be restricted to administrative users. The `Principal` object is used to track which administrator created a specific token.
*   **Token Security**: The `create` method returns the raw token string. Ensure that the transport layer is secured via HTTPS to prevent token interception.
*   **Lifecycle**: Tokens are managed via the `AgentTokenService`. Revoking a token immediately invalidates it for future agent authentication requests.
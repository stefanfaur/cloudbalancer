# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AgentRegistrationController.java

## Overview

The `AgentRegistrationController` is a Spring `@RestController` responsible for handling the initial handshake and registration process for worker agents within the CloudBalancer infrastructure. It provides a secure endpoint that validates agent credentials and returns the necessary configuration parameters (such as Kafka broker details) required for the agent to join the message bus.

## Public API

### `AgentRegistrationController`

*   **`AgentRegistrationController(AgentTokenService tokenService, RegistrationProperties registrationProps)`**
    *   Constructor-based dependency injection for the token validation service and registration configuration properties.

*   **`ResponseEntity<AgentRegistrationResponse> register(@RequestBody AgentRegistrationRequest request)`**
    *   **Endpoint**: `POST /api/agents/register`
    *   **Description**: Processes an incoming registration request. It validates the provided token via `AgentTokenService`. If valid, it returns an `AgentRegistrationResponse` containing Kafka connection credentials.
    *   **Returns**: 
        *   `200 OK` with `AgentRegistrationResponse` body upon successful registration.
        *   `401 Unauthorized` if the provided token is invalid or revoked.

## Dependencies

*   **`com.cloudbalancer.dispatcher.api.dto.AgentRegistrationRequest`**: Data transfer object containing agent identification and hardware specifications.
*   **`com.cloudbalancer.dispatcher.api.dto.AgentRegistrationResponse`**: Data transfer object containing Kafka connection details.
*   **`com.cloudbalancer.dispatcher.registration.AgentTokenService`**: Service used to verify the authenticity of the registration token.
*   **`com.cloudbalancer.dispatcher.registration.RegistrationProperties`**: Configuration properties provider for Kafka broker settings.
*   **Spring Web MVC**: Provides the `@RestController` and `@PostMapping` annotations for request handling.

## Usage Notes

*   **Security**: This controller acts as the gatekeeper for new agents. Ensure that the `AgentTokenService` implementation is correctly configured to verify tokens against your security provider.
*   **Logging**: The controller logs registration attempts at the `WARN` level for failures and `INFO` level for successful registrations, including the agent's reported hardware capacity (CPU and Memory).
*   **Configuration**: The Kafka connection details returned to the agent are sourced from `RegistrationProperties`. Ensure these properties are correctly injected via the application's environment configuration (e.g., `application.yml` or environment variables).
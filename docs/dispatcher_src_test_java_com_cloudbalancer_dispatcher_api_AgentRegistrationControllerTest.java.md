# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AgentRegistrationControllerTest.java

## Overview

`AgentRegistrationControllerTest` is a Spring Boot integration test suite designed to validate the security and functional requirements of the agent registration endpoint (`/api/agents/register`). The test class ensures that the `AgentRegistrationController` correctly handles token-based authentication, token revocation, and idempotent registration requests.

This suite utilizes `MockMvc` to simulate HTTP requests and `TestContainersConfig` to provide a realistic environment for verifying interactions with the underlying token service and registration logic.

## Public API

The class contains the following test methods:

*   **`register_withValidToken_returnsKafkaCredentials`**: Verifies that a request with a valid, active registration token returns a 200 OK status along with the required Kafka connection credentials (bootstrap server, username, and password).
*   **`register_withInvalidToken_returns401`**: Ensures that requests using non-existent or malformed tokens are rejected with an HTTP 401 Unauthorized status.
*   **`register_withRevokedToken_returns401`**: Confirms that tokens previously revoked via `AgentTokenService` are no longer accepted, resulting in an HTTP 401 Unauthorized status.
*   **`register_withDuplicateAgentId_sameToken_succeeds`**: Validates the idempotency of the registration process by ensuring that multiple registration attempts with the same agent ID and token are handled successfully.
*   **`registrationBody(String agentId, String token)`**: A private helper method used to generate the JSON payload required for registration requests.

## Dependencies

*   **JUnit 5**: Used as the primary testing framework.
*   **Spring Boot Test**: Provides the `@SpringBootTest` and `@AutoConfigureMockMvc` annotations for full application context testing.
*   **MockMvc**: Used for performing simulated HTTP requests to the controller.
*   **AgentTokenService**: Injected to manage the lifecycle of registration tokens during test execution.
*   **TestContainersConfig**: Injected to configure necessary containerized infrastructure required by the dispatcher module.

## Usage Notes

*   **Environment**: These tests require a running application context and are designed to be executed as part of the standard Maven/Gradle build lifecycle.
*   **Test Data**: The `registrationBody` helper method creates a standard JSON payload containing `agentId`, `token`, `cpuCores`, and `memoryMb`. Changes to the registration request schema in the production controller must be reflected in this helper method.
*   **Isolation**: Each test case manages its own token lifecycle via the `AgentTokenService` to ensure that tests remain independent and do not suffer from state pollution.
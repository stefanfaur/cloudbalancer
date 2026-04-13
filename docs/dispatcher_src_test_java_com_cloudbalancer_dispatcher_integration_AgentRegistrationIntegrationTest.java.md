# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/AgentRegistrationIntegrationTest.java

## Overview

`AgentRegistrationIntegrationTest` is an integration test suite designed to validate the end-to-end lifecycle of agent registration within the `dispatcher` module. It ensures that the interaction between administrative token management and agent registration endpoints functions correctly under realistic conditions.

The test suite leverages `MockMvc` to simulate HTTP requests and `TestContainersConfig` to provide a containerized environment, ensuring that the dispatcher's security, token management, and registration logic are verified against actual infrastructure dependencies.

## Public API

### `AgentRegistrationIntegrationTest`

The class serves as an integration test suite and does not expose a public API for production use. It is intended for execution within the CI/CD pipeline to verify system integrity.

### `fullFlow_createToken_register_revoke_reject()`

This test method executes a four-step integration scenario:
1. **Token Creation**: Uses an admin-authorized request to generate a new registration token.
2. **Registration**: Uses the generated token to register an agent, verifying that the dispatcher returns the necessary Kafka connection details.
3. **Revocation**: Performs an administrative action to revoke the previously generated token.
4. **Rejection**: Attempts to register an agent using the now-revoked token, asserting that the system correctly returns an `401 Unauthorized` status.

## Dependencies

- **JUnit 5**: Used for test orchestration and assertions.
- **Spring Boot Test**: Provides the testing context, including `@SpringBootTest` and `@AutoConfigureMockMvc`.
- **TestContainers**: Configured via `TestContainersConfig` to provide necessary infrastructure (e.g., databases or message brokers) required for the dispatcher to function.
- **Jackson Databind**: Used for parsing JSON responses from the `MockMvc` results.
- **JwtService**: Used to generate valid administrative JWTs for protected endpoint access.

## Usage Notes

- **Environment**: This test requires a functional Docker environment to spin up the containers defined in `TestContainersConfig`.
- **Security**: The test assumes the presence of a valid `JwtService` bean. The `adminToken` is generated dynamically during the test to simulate a real-world administrative session.
- **Integration Scope**: This test covers the `AgentController` and the underlying security/registration services. It is an essential check for verifying that token revocation logic is correctly enforced at the API gateway level.
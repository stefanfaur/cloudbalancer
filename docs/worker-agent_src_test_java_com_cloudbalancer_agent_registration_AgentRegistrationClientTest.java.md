# File: worker-agent/src/test/java/com/cloudbalancer/agent/registration/AgentRegistrationClientTest.java

## Overview

`AgentRegistrationClientTest` is a unit test suite for the `AgentRegistrationClient` class. Its primary purpose is to verify the behavior of the agent registration process when provided with invalid or missing authentication tokens. The tests ensure that the client gracefully handles missing configuration by returning `null` rather than throwing exceptions or attempting invalid network requests.

## Public API

### `AgentRegistrationClientTest`

The class contains the following test methods:

*   **`register_withNoToken_returnsNull()`**: Verifies that when the `AgentProperties` object contains an empty string (`""`) as the registration token, the `register()` method returns `null`.
*   **`register_withNullToken_returnsNull()`**: Verifies that when the `AgentProperties` object contains a `null` value for the registration token, the `register()` method returns `null`.

## Dependencies

*   **`com.cloudbalancer.agent.config.AgentProperties`**: Used to configure the registration token for the client under test.
*   **`com.fasterxml.jackson.databind.ObjectMapper`**: Used by the `AgentRegistrationClient` for JSON serialization/deserialization.
*   **`org.junit.jupiter.api.Test`**: JUnit 5 testing framework.
*   **`org.assertj.core.api.Assertions`**: Used for fluent assertion syntax.

## Usage Notes

*   These tests are intended to validate the defensive programming logic within `AgentRegistrationClient`. 
*   The tests assume that the `AgentRegistrationClient` constructor correctly accepts `AgentProperties` and `ObjectMapper` as dependencies.
*   When running these tests, ensure that the `AgentProperties` class is correctly configured in the classpath, as the tests rely on its setter methods to simulate various configuration states.
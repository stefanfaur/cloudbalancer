# File: worker-agent/src/test/java/com/cloudbalancer/agent/kafka/AgentHeartbeatPublisherTest.java

## Overview

`AgentHeartbeatPublisherTest` is a JUnit 5 test suite designed to verify the functionality of the `AgentHeartbeatPublisher` class. It ensures that the worker agent correctly communicates its registration status and periodic heartbeats to the Kafka message broker. The tests validate that the published messages contain accurate configuration data and current container capacity information.

## Public API

The class contains the following test methods:

*   **`setUp()`**: Initializes the mock environment, including `KafkaTemplate` and `ContainerManager`, and configures default `AgentProperties` before each test execution.
*   **`publishRegistrationSendsToCorrectTopic()`**: Verifies that the registration event is sent to the `agents.registration` Kafka topic and that the serialized JSON payload contains the correct agent metadata.
*   **`publishHeartbeatIncludesCapacityInfo()`**: Verifies that the heartbeat event is sent to the `agents.heartbeat` Kafka topic and that it correctly reflects the active worker IDs managed by the `ContainerManager`.

## Dependencies

The test suite relies on the following components:

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Mockito**: Used for mocking `KafkaTemplate` and `ContainerManager` to simulate interactions with the messaging layer and system state.
*   **AssertJ**: Used for fluent assertion syntax.
*   **Spring Kafka**: Provides the `KafkaTemplate` interface for message publishing.
*   **CloudBalancer Common**: Utilizes `AgentHeartbeat`, `AgentRegisteredEvent`, and `JsonUtil` for message structure and serialization.
*   **AgentProperties**: Provides the configuration context for the agent (ID, hostname, capacity).

## Usage Notes

*   **Mocking Strategy**: The `KafkaTemplate` is mocked to prevent actual network calls during testing. The `ArgumentCaptor` is used to intercept the JSON strings passed to the template, which are then deserialized to verify the integrity of the message content.
*   **Test Environment**: The `setUp` method configures a static agent identity (`agent-1`) and predefined hardware resources (8 cores, 16GB RAM) to ensure consistent test results.
*   **Verification**: Tests specifically check for the correct Kafka topic routing and the presence of expected fields in the JSON payloads, ensuring the contract between the agent and the dispatcher/controller remains intact.
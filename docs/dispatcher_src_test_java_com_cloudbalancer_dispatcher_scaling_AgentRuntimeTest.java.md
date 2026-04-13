# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scaling/AgentRuntimeTest.java

## Overview

`AgentRuntimeTest` is a comprehensive JUnit 5 test suite designed to validate the `AgentRuntime` class in the `dispatcher` module. This class is responsible for orchestrating worker lifecycle operations (start, stop, and drain) by interacting with the `AgentRegistry` and publishing commands to Kafka.

**Note:** This file is identified as a **hotspot** due to its high change frequency and central role in the dispatcher's scaling logic. It is a high-risk area for regressions; changes to the command serialization or agent selection logic should be thoroughly verified against these tests.

## Public API

The test suite validates the following core operations of `AgentRuntime`:

*   **`startWorker(WorkerConfig config)`**: Verifies that the runtime correctly selects an available agent based on resource requirements and publishes a `StartWorkerCommand` to the appropriate Kafka topic.
*   **`stopWorker(String workerId)`**: Ensures that a `StopWorkerCommand` is dispatched to the agent hosting the specified worker.
*   **`drainAndStop(String workerId, int timeout)`**: Validates that the runtime correctly signals an agent to drain a worker within a specified timeframe.

## Dependencies

The test suite relies on the following components:
*   **`KafkaTemplate`**: Mocked to verify outgoing command messages.
*   **`AgentRegistry`**: Used to maintain the state of available agents and their current workloads.
*   **`PendingWorkerTracker`**: Tracks workers that have been assigned but not yet acknowledged, ensuring the dispatcher does not over-provision.
*   **`JsonUtil`**: Used to deserialize and validate the structure of JSON commands sent over Kafka.

## Usage Notes

### Implementation Rationale
The tests use `ArgumentCaptor` to inspect the JSON payload sent to Kafka. This is critical because the `AgentRuntime` abstracts the command creation process; verifying the serialized output ensures that the contract between the dispatcher and the worker agents remains intact.

### Testing Edge Cases
*   **No Available Agents**: The `startWorkerReturnsFalseWhenNoAgentAvailable` test ensures the system gracefully handles scenarios where no agents meet the resource requirements, preventing invalid command dispatch.
*   **Command Serialization**: By deserializing the captured Kafka message back into `AgentCommand` objects, the tests verify that the `AgentRuntime` correctly maps internal logic to the expected polymorphic command types (`StartWorkerCommand`, `StopWorkerCommand`).

### Common Pitfalls
*   **Registry State**: When adding new tests, ensure the `AgentRegistry` is populated with a valid `AgentHeartbeat` that matches the `WorkerConfig` requirements (e.g., `ExecutorType`, CPU, and Memory). Failure to do so will result in the `startWorker` method returning `false`.
*   **Kafka Topic/Key**: The tests explicitly check for the `agents.commands` topic and the specific agent ID as the Kafka key. Ensure that any changes to the messaging architecture are reflected in these assertions.

### Example: Verifying a New Command
To test a new command type, follow the pattern established in `drainAndStopPublishesDrainCommand`:
1.  Setup the `AgentRegistry` with a mock agent.
2.  Invoke the `AgentRuntime` method.
3.  Use `ArgumentCaptor` to capture the string sent to `kafkaTemplate.send()`.
4.  Use `JsonUtil.mapper().readValue()` to convert the string to the expected `AgentCommand` subclass.
5.  Assert the specific fields of the command object.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scaling/AgentRegistryTest.java

## Overview

`AgentRegistryTest` is a JUnit 5 test suite designed to validate the functionality of the `AgentRegistry` class within the dispatcher module. The suite ensures that the registry correctly tracks agent availability, manages resource capacity, and handles stale agent removal based on heartbeat signals.

## Public API

The test class does not expose a public API as it is a test suite. However, it exercises the following methods of the `AgentRegistry` class:

*   **`updateAgent(AgentHeartbeat heartbeat)`**: Verifies that new agents are correctly registered and existing agent data is updated.
*   **`selectBestHost(WorkerConfig config)`**: Validates the logic for selecting an agent based on available CPU and memory resources.
*   **`markDeadIfStale(Duration threshold)`**: Ensures that agents failing to provide a heartbeat within the specified duration are removed from the registry.
*   **`getAliveAgents()`**: Used to verify the internal state of the registry after operations.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management (`@BeforeEach`, `@Test`).
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **`com.cloudbalancer.common`**: Provides shared models including `AgentHeartbeat`, `ExecutorType`, and `WorkerConfig`.
*   **`com.cloudbalancer.dispatcher.scaling.AgentRegistry`**: The system under test.

## Usage Notes

*   **Test Setup**: Each test method initializes a fresh `AgentRegistry` instance via the `@BeforeEach` `setUp` method to ensure test isolation.
*   **Helper Methods**: The `heartbeat(...)` private helper method is used to generate mock `AgentHeartbeat` objects with configurable resource metrics, simplifying the creation of test scenarios.
*   **Resource Matching**: Tests verify that `selectBestHost` correctly filters agents by capacity; if no agent meets the `WorkerConfig` requirements, the method returns an empty `Optional`.
*   **Staleness Logic**: The `markDeadIfStaleRemovesStaleAgents` test manually manipulates the `lastHeartbeat` timestamp of an agent to simulate network partitions or agent failures, verifying the registry's cleanup mechanism.
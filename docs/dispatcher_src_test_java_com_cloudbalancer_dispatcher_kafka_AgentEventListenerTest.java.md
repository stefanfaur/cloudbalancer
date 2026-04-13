# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/kafka/AgentEventListenerTest.java

## Overview

`AgentEventListenerTest` is a JUnit 5 test suite responsible for verifying the event processing logic within the `AgentEventListener` class. It ensures that the dispatcher correctly handles incoming Kafka events from agents, including heartbeats, worker lifecycle transitions (started/failed), and worker termination events. The tests validate the integration between the event listener, the `AgentRegistry`, the `PendingWorkerTracker`, and the `WorkerRegistryService`.

## Public API

The class contains the following test methods:

*   **`setUp()`**: Initializes the test environment, including mocks for `WorkerRegistryService`, `EventPublisher`, and `WorkerFailureHandler`, and instantiates the `AgentEventListener` with real instances of `AgentRegistry` and `PendingWorkerTracker`.
*   **`heartbeatUpdatesAgentRegistry()`**: Validates that an `AgentHeartbeat` JSON payload is correctly parsed and results in the agent being added to the `AgentRegistry`.
*   **`workerStartedEventResolvesTrackerAndRegistersWorker()`**: Verifies that a `WorkerStartedEvent` clears the worker from the `PendingWorkerTracker` and triggers a registration in the `WorkerRegistryService`.
*   **`workerStartFailedEventResolvesTracker()`**: Confirms that a `WorkerStartFailedEvent` correctly removes the worker from the `PendingWorkerTracker`.
*   **`workerStoppedEventMarksWorkerDead()`**: Ensures that a `WorkerStoppedEvent` triggers the `markDead` operation in the `WorkerRegistryService`.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **Mockito**: Used for mocking service-layer dependencies (`WorkerRegistryService`, `EventPublisher`, `WorkerFailureHandler`).
*   **AssertJ**: Used for fluent assertion syntax.
*   **Internal Modules**:
    *   `com.cloudbalancer.common.agent.*`: Models for agent events and heartbeats.
    *   `com.cloudbalancer.dispatcher.scaling.*`: `AgentRegistry` and `PendingWorkerTracker` state management.
    *   `com.cloudbalancer.dispatcher.service.*`: `WorkerRegistryService` and `WorkerFailureHandler` for business logic.

## Usage Notes

*   **Mocking Strategy**: The test uses a hybrid approach where stateful components (`AgentRegistry`, `PendingWorkerTracker`) are used as real objects to verify state transitions, while external services are mocked to isolate the listener's logic.
*   **JSON Serialization**: Tests utilize `JsonUtil.mapper()` to simulate the actual Kafka message consumption process, ensuring that the `AgentEventListener` correctly handles the deserialization of incoming event strings.
*   **Execution**: These tests should be run as part of the standard Maven/Gradle build lifecycle. They require no external infrastructure (e.g., Kafka brokers) as all dependencies are mocked or in-memory.
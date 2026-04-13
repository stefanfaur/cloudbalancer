# File: worker/src/test/java/com/cloudbalancer/worker/kafka/WorkerCommandListenerTest.java

## Overview

`WorkerCommandListenerTest` is a unit test suite for the `WorkerCommandListener` class, responsible for verifying how the worker node processes Kafka-based administrative commands. Specifically, it ensures that the worker correctly interprets `DrainCommand` messages and updates its internal state (the `drainingFlag`) only when the command is explicitly targeted at the current worker instance.

## Public API

The class contains the following test methods:

*   **`drainCommandSetsDrainingFlag()`**: Verifies that when a `DrainCommand` is received with a matching worker ID, the `AtomicBoolean` draining flag is successfully toggled to `true`.
*   **`drainCommandForOtherWorkerIgnored()`**: Verifies that when a `DrainCommand` is received with a worker ID that does not match the current instance, the `drainingFlag` remains `false`, ensuring commands are scoped correctly to specific workers.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management and assertions.
*   **Mockito**: Used via `MockitoExtension` to support mocking and test context management.
*   **AssertJ**: Provides fluent assertions (`assertThat`) for verifying the state of the `AtomicBoolean` flag.
*   **`com.cloudbalancer.common.model.DrainCommand`**: The data model being processed by the listener.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Used to serialize `DrainCommand` objects into JSON strings to simulate incoming Kafka messages.

## Usage Notes

*   **Test Isolation**: Each test initializes a new `WorkerCommandListener` with a local `AtomicBoolean` instance, ensuring that test cases do not share state.
*   **JSON Simulation**: The tests simulate the Kafka consumer's input by serializing the `DrainCommand` object into a JSON string, mimicking the actual payload format expected by the `onCommand` method.
*   **Execution**: These tests are intended to be run as part of the standard Maven/Gradle build lifecycle to ensure that command processing logic remains robust against regressions.
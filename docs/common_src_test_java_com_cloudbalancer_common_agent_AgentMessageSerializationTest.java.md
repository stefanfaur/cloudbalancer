# File: common/src/test/java/com/cloudbalancer/common/agent/AgentMessageSerializationTest.java

## Overview

`AgentMessageSerializationTest` is a critical unit test suite responsible for verifying the JSON serialization and deserialization integrity of the core agent-to-dispatcher communication models. It ensures that all `AgentCommand`, `AgentEvent`, and heartbeat data structures maintain their state and type information when converted to and from JSON strings using the system's `JsonUtil` configuration.

**Note:** This file is a **HOTSPOT** within the `common` module. Due to its high change frequency and complexity, it is considered a high-risk area for bugs. Any changes to the underlying data models (e.g., adding fields to `AgentHeartbeat` or `AgentCommand`) must be reflected here to prevent runtime serialization failures in production.

## Public API

The class provides test coverage for the following message types via "round-trip" testing (serializing an object to JSON and immediately deserializing it back to an object):

*   **`agentHeartbeatRoundTrip`**: Validates `AgentHeartbeat` serialization, ensuring resource metrics (CPU, RAM) and worker status lists are preserved.
*   **`startWorkerCommandRoundTrip`**: Validates `AgentCommand.StartWorkerCommand` serialization, specifically checking environment variables and executor configuration.
*   **`stopWorkerCommandRoundTrip`**: Validates `AgentCommand.StopWorkerCommand` serialization, focusing on drain flags and timeout settings.
*   **`workerStartedEventRoundTrip`**: Validates `AgentEvent.WorkerStartedEvent` serialization, ensuring container identifiers are correctly mapped.
*   **`workerStartFailedEventRoundTrip`**: Validates `AgentEvent.WorkerStartFailedEvent` serialization, ensuring error messages are preserved.
*   **`workerStoppedEventRoundTrip`**: Validates `AgentEvent.WorkerStoppedEvent` serialization.
*   **`agentRegisteredEventRoundTrip`**: Validates `AgentRegisteredEvent` serialization, ensuring initial agent capacity metrics are correctly transmitted.

## Dependencies

*   **`com.cloudbalancer.common.util.JsonUtil`**: The primary utility used for object mapping. This test implicitly verifies that `JsonUtil` is correctly configured to handle polymorphic types (e.g., `AgentCommand` and `AgentEvent` subclasses).
*   **`com.cloudbalancer.common.model.ExecutorType`**: Used to define executor constraints in commands and heartbeats.
*   **`org.junit.jupiter.api.Test`**: JUnit 5 framework for test execution.
*   **`org.assertj.core.api.Assertions`**: Used for fluent assertion syntax.

## Usage Notes

### Implementation Rationale
The test suite uses "round-trip" testing to ensure that the Jackson configuration (defined in `JsonUtil`) correctly handles:
1.  **Polymorphism**: `AgentCommand` and `AgentEvent` are likely abstract classes or interfaces. The tests verify that the JSON output includes the necessary type information (e.g., `@JsonTypeInfo`) to allow the deserializer to instantiate the correct concrete subclass.
2.  **Temporal Data**: The use of `java.time.Instant` in these models requires specific Jackson modules (e.g., `jackson-datatype-jsr310`). These tests confirm that `Instant` objects are serialized into ISO-8601 strings and back without precision loss.

### Potential Pitfalls
*   **Schema Evolution**: If a field is added to a model without a default value or proper Jackson annotation, the round-trip test will fail. Always update these tests when modifying the `Agent` model classes.
*   **Polymorphic Registration**: If a new `AgentCommand` subclass is added, it must be registered with the Jackson mapper (usually via annotations on the base class). Failure to do so will result in a `JsonMappingException` during the round-trip test.

### Example: Adding a new Message Type
If you introduce a new `AgentEvent` subclass, you must add a corresponding test method to this file:

```java
@Test
void newEventRoundTrip() throws Exception {
    AgentEvent event = new AgentEvent.NewEvent("agent-1", "data");
    String json = JsonUtil.mapper().writeValueAsString(event);
    var deserialized = JsonUtil.mapper().readValue(json, AgentEvent.class);
    
    assertThat(deserialized).isInstanceOf(AgentEvent.NewEvent.class);
    assertThat(((AgentEvent.NewEvent) deserialized).data()).isEqualTo("data");
}
```

This ensures that the new event type is correctly handled by the system's serialization infrastructure before it is deployed to production.
# File: worker-agent/src/test/java/com/cloudbalancer/agent/kafka/AgentCommandListenerTest.java

## Overview

`AgentCommandListenerTest` is a JUnit 5 test suite designed to validate the command processing logic of the `AgentCommandListener` component. This class is responsible for interpreting Kafka messages containing `AgentCommand` objects and coordinating with the `ContainerManager` to perform lifecycle operations (start/stop) on worker containers.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It represents a high-risk area for bugs, as changes here directly impact the agent's ability to respond to infrastructure orchestration commands.

## Public API

The test suite validates the following primary interaction patterns via the `AgentCommandListener`:

- `onCommand(String json)`: The entry point for processing incoming Kafka messages.
    - **StartWorkerCommand**: Triggers `containerManager.startWorker(...)` and publishes a `WorkerStartedEvent` upon success or `WorkerStartFailedEvent` upon failure.
    - **StopWorkerCommand**: Triggers `containerManager.stopWorker(...)` and publishes a `WorkerStoppedEvent`.
    - **Filtering**: Commands targeting a different `agentId` are ignored, ensuring agents only process their assigned tasks.

## Dependencies

The test suite relies on the following core components:

- **Mockito**: Used for mocking `KafkaTemplate` and `ContainerManager` to isolate the listener logic.
- **AssertJ**: Used for fluent assertions on event types and properties.
- **AgentProperties**: Provides the configuration context (specifically the `agentId`) used to filter incoming commands.
- **JsonUtil**: Used for serializing/deserializing `AgentCommand` and `AgentEvent` objects to simulate Kafka payloads.
- **Spring Kafka**: The `KafkaTemplate` is mocked to verify that events are correctly published to the `agents.events` topic.

## Usage Notes

### Implementation Rationale
The tests use `ArgumentCaptor` to inspect the JSON payloads sent to the `KafkaTemplate`. This is critical because the listener operates on serialized strings, and the tests must ensure the resulting `AgentEvent` objects are correctly structured for downstream consumers.

### Edge Cases and Pitfalls
1. **Error Handling**: The `startWorkerCommandPublishesFailedEventOnError` test ensures that exceptions thrown by the `ContainerManager` (e.g., image pull failures) do not crash the listener but are instead reported back to the system via a failure event.
2. **Filtering Logic**: The `commandForDifferentAgentIsIgnored` test is vital for multi-agent environments. If the `agentId` in the command does not match the local `AgentProperties.id`, the listener must perform no actions to prevent cross-contamination of container management tasks.
3. **Serialization**: Since the listener relies on `JsonUtil`, ensure that any changes to the `AgentCommand` or `AgentEvent` class hierarchies are reflected in the test data to avoid `JsonMappingException` during test execution.

### Example: Testing a New Command
To add a new command type to the listener, follow this pattern:
1. Define the command in `AgentCommand`.
2. Implement the handler in `AgentCommandListener`.
3. Add a test case in `AgentCommandListenerTest`:
   ```java
   @Test
   void newCommandTypeExecutesSuccessfully() throws Exception {
       // 1. Prepare command
       var cmd = new AgentCommand.NewCommand(...);
       // 2. Execute
       listener.onCommand(JsonUtil.mapper().writeValueAsString(cmd));
       // 3. Verify interaction
       verify(containerManager).performNewAction(...);
       // 4. Verify event publication
       verify(kafkaTemplate).send(eq("agents.events"), anyString(), anyString());
   }
   ```
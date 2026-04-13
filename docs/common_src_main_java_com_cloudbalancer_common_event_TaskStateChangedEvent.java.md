# File: common/src/main/java/com/cloudbalancer/common/event/TaskStateChangedEvent.java

## Overview

The `TaskStateChangedEvent` is an immutable Java `record` used within the CloudBalancer system to represent a state transition for a specific task. It acts as a standardized event payload, facilitating communication across the system when a task moves from one lifecycle phase to another.

This event captures the context of the transition, including the task identifier, the previous and new states, and a descriptive reason for the change, ensuring auditability and observability of task lifecycles.

## Public API

### `TaskStateChangedEvent` (Record)

*   **`eventId` (String)**: A unique identifier for the event instance.
*   **`timestamp` (Instant)**: The precise time at which the state change occurred.
*   **`taskId` (UUID)**: The unique identifier of the task that underwent the state change.
*   **`previousState` (TaskState)**: The state of the task prior to this event.
*   **`newState` (TaskState)**: The state of the task following this event.
*   **`reason` (String)**: A human-readable explanation or code describing why the state transition occurred.

### Methods

#### `eventType()`
Returns the constant string identifier for this event type.
*   **Returns**: `"TASK_STATE_CHANGED"`

## Dependencies

*   `com.cloudbalancer.common.model.TaskState`: Used to define the `previousState` and `newState` fields.
*   `java.time.Instant`: Used for event timestamping.
*   `java.util.UUID`: Used for uniquely identifying the task.
*   `CloudBalancerEvent` (Interface): The record implements this interface to participate in the system's event-driven architecture.

## Usage Notes

*   **Immutability**: As a Java `record`, this event is immutable. Once instantiated, the state transition details cannot be modified, ensuring thread safety and consistency when passing the event through message queues or event buses.
*   **Event Handling**: Consumers of this event should use the `eventType()` method to filter or route events within the system's event-processing pipeline.
*   **Integration**: This event is typically emitted by the task management service whenever a `TaskState` update is persisted to the database or triggered by an external worker signal.
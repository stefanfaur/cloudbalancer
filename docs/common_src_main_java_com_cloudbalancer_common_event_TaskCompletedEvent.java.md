# File: common/src/main/java/com/cloudbalancer/common/event/TaskCompletedEvent.java

## Overview

The `TaskCompletedEvent` is an immutable Java `record` used within the CloudBalancer system to encapsulate the result of a completed task execution. It acts as a standardized event payload to notify the system that a specific task has finished, providing the necessary metadata and execution output for downstream processing, logging, or state updates.

## Public API

### `TaskCompletedEvent` (Record)

*   **`eventId`**: A unique identifier for the event instance.
*   **`timestamp`**: The `Instant` at which the task completion event occurred.
*   **`taskId`**: The `UUID` of the task that has completed.
*   **`exitCode`**: The integer status code returned by the task process.
*   **`stdout`**: The standard output captured from the task execution.
*   **`stderr`**: The standard error captured from the task execution.

### Methods

#### `eventType()`
Returns the constant string identifier for this event type.
*   **Returns**: `String` - Always returns `"TASK_COMPLETED"`.

## Dependencies

*   `java.time.Instant`: Used for precise event timestamping.
*   `java.util.UUID`: Used for unique identification of the associated task.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure type consistency across the event-driven architecture.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified, ensuring thread safety when passing events through the system's event bus or message queues.
*   **Integration**: This event is intended to be consumed by components responsible for task lifecycle management, such as the orchestrator or monitoring services, to determine if a task succeeded or failed based on the `exitCode`.
*   **Serialization**: Given its structure, this record is suitable for serialization into JSON or other formats for inter-service communication within the CloudBalancer infrastructure.
# File: common/src/main/java/com/cloudbalancer/common/event/TaskAssignedEvent.java

## Overview

The `TaskAssignedEvent` is an immutable data carrier implemented as a Java `record`. It serves as a formal event notification within the CloudBalancer system, signaling that a specific task has been successfully assigned to a designated worker node. This event is part of the system's event-driven architecture, facilitating communication between task management and worker orchestration components.

## Public API

### `TaskAssignedEvent` (Record)

*   **`eventId` (String)**: A unique identifier for the event instance.
*   **`timestamp` (Instant)**: The exact time the assignment event occurred.
*   **`taskId` (UUID)**: The unique identifier of the task being assigned.
*   **`workerId` (String)**: The identifier of the worker node to which the task is assigned.

### Methods

*   **`eventType()`**: Returns the constant string `"TASK_ASSIGNED"`. This method is implemented as part of the `CloudBalancerEvent` interface contract to allow for polymorphic event handling.

## Dependencies

*   `java.time.Instant`: Used for precise event timestamping.
*   `java.util.UUID`: Used for unique identification of the task.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure type consistency across the event system.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified, ensuring thread safety and consistency when passing events through message queues or event buses.
*   **Event Handling**: Consumers of this event should use the `eventType()` method to filter or route events appropriately within the system's event processor.
*   **Integration**: This event is typically emitted by the task scheduler component once a worker selection logic has successfully matched a task to an available worker.
# File: common/src/main/java/com/cloudbalancer/common/event/TaskSubmittedEvent.java

## Overview

The `TaskSubmittedEvent` is an immutable data carrier implemented as a Java `record`. It serves as a formal event notification within the CloudBalancer system, signaling that a new task has been submitted for processing. This event encapsulates the necessary metadata, including a unique event identifier, the submission timestamp, the associated task ID, and the task's configuration details.

## Public API

### `TaskSubmittedEvent` (Record)

*   **`eventId`**: A `String` representing the unique identifier for the event instance.
*   **`timestamp`**: An `Instant` representing the precise time the task was submitted.
*   **`taskId`**: A `UUID` identifying the specific task being submitted.
*   **`descriptor`**: A `TaskDescriptor` object containing the configuration and requirements of the task.

### Methods

*   **`eventType()`**: Returns the constant string `"TASK_SUBMITTED"`, identifying the nature of this event within the event-driven architecture.

## Dependencies

*   `com.cloudbalancer.common.model.TaskDescriptor`: Used to define the structure and requirements of the submitted task.
*   `java.time.Instant`: Used for precise event timestamping.
*   `java.util.UUID`: Used for unique task identification.
*   `CloudBalancerEvent` (Interface): The base interface implemented by this record to ensure type safety within the event bus.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified, ensuring thread safety when passing events through asynchronous message queues or event buses.
*   **Event Handling**: Consumers of this event should use the `eventType()` method to filter or route the event to the appropriate handler logic.
*   **Integration**: This event is typically emitted by the task submission service and consumed by schedulers or monitoring components within the CloudBalancer infrastructure.
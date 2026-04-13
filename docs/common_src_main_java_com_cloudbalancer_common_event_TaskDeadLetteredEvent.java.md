# File: common/src/main/java/com/cloudbalancer/common/event/TaskDeadLetteredEvent.java

## Overview

The `TaskDeadLetteredEvent` is a Java `record` used within the CloudBalancer system to represent an event where a specific task has failed repeatedly and has been moved to a dead-letter queue. This event captures the state of the task at the time of failure, including the history of execution attempts and the specific reason for the dead-lettering.

## Public API

### `eventType()`
Returns the unique string identifier for this event type.

*   **Signature**: `public String eventType()`
*   **Returns**: `"TASK_DEAD_LETTERED"`

### Record Components
*   `eventId` (String): A unique identifier for the event instance.
*   `timestamp` (Instant): The time at which the task was moved to the dead-letter queue.
*   `taskId` (UUID): The unique identifier of the task that failed.
*   `reason` (String): A descriptive message explaining why the task was dead-lettered.
*   `attemptCount` (int): The total number of execution attempts made before the task was dead-lettered.
*   `executionHistory` (List<ExecutionAttempt>): A list of `ExecutionAttempt` objects detailing the history of the task's execution lifecycle.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutionAttempt`: Used to track the history of task execution attempts.
*   `java.time.Instant`: Used for timestamping the event.
*   `java.util.List`: Used to store the collection of execution attempts.
*   `java.util.UUID`: Used to uniquely identify the task.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure compatibility with the event processing system.

## Usage Notes

*   This class is immutable, consistent with its implementation as a Java `record`.
*   The `TaskDeadLetteredEvent` is intended for use in asynchronous event-driven architectures where monitoring systems or manual intervention workflows need to be notified of task failures that exceed retry thresholds.
*   Ensure that the `reason` field is populated with meaningful diagnostic information to facilitate troubleshooting.
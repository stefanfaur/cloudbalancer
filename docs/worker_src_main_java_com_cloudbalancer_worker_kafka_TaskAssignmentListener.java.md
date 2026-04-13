# File: worker/src/main/java/com/cloudbalancer/worker/kafka/TaskAssignmentListener.java

## Overview

The `TaskAssignmentListener` is a Spring `@Component` responsible for consuming task assignment messages from a Kafka topic. It acts as the primary entry point for incoming work on the worker node, filtering assignments based on the worker's unique identifier and delegating execution to the `TaskExecutionService`.

The component is designed to be lifecycle-aware, respecting a "draining" state that prevents the worker from accepting new tasks during shutdown or maintenance periods.

## Public API

### `TaskAssignmentListener` (Constructor)
Initializes the listener with the required execution service, draining state, and worker identity.

*   **Parameters**:
    *   `TaskExecutionService executionService`: Service responsible for executing the assigned tasks.
    *   `AtomicBoolean draining`: A thread-safe flag indicating if the worker is currently in a draining state.
    *   `String workerId`: The unique identifier for this worker instance, injected via the `cloudbalancer.worker.id` property (defaults to `worker-1`).

### `onTaskAssigned(String message)`
A Kafka listener method annotated with `@KafkaListener`. It processes incoming JSON messages from the `tasks.assigned` topic.

*   **Logic**:
    1.  Checks the `draining` flag; if `true`, the message is ignored.
    2.  Deserializes the JSON message into a `TaskAssignment` object.
    3.  Verifies if the `assignedWorkerId` matches the current instance's `workerId`.
    4.  If the ID matches, it invokes `executionService.executeTask(assignment)`.
*   **Exceptions**: Catches and logs any deserialization or execution errors to prevent the Kafka consumer from crashing.

## Dependencies

*   **`com.cloudbalancer.common.model.TaskAssignment`**: Data model for task metadata.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Utility for JSON serialization/deserialization.
*   **`com.cloudbalancer.worker.service.TaskExecutionService`**: Business logic service for task processing.
*   **`org.springframework.kafka.annotation.KafkaListener`**: Spring Kafka integration for message consumption.
*   **`java.util.concurrent.atomic.AtomicBoolean`**: Used to manage the thread-safe draining state.

## Usage Notes

*   **Kafka Configuration**: The listener subscribes to the `tasks.assigned` topic. The `groupId` is dynamically set to the `workerId`, ensuring that each worker instance maintains its own consumer offset if configured as a unique group, or participates in a consumer group as defined by the deployment architecture.
*   **Draining State**: When the `draining` flag is set to `true` (typically during application shutdown), the listener will stop processing new tasks. This ensures graceful termination without interrupting tasks currently in progress.
*   **Filtering**: The listener performs an early exit if the `assignedWorkerId` does not match the current worker's ID. This is critical in a distributed environment where multiple workers may be listening to the same topic.
*   **Error Handling**: Errors during message processing are logged but do not propagate, ensuring the Kafka listener container remains active.
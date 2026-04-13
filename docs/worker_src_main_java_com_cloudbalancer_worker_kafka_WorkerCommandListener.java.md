# File: worker/src/main/java/com/cloudbalancer/worker/kafka/WorkerCommandListener.java

## Overview

The `WorkerCommandListener` is a Spring `@Component` responsible for processing control commands sent to the worker node via Apache Kafka. It acts as a command consumer, listening to the `workers.commands` topic to manage worker lifecycle events, such as graceful shutdown or draining.

The component is designed to filter incoming messages based on a unique `workerId`, ensuring that each worker instance only reacts to commands explicitly addressed to it.

## Public API

### `WorkerCommandListener` (Constructor)
Initializes the listener with the worker's identity and a shared state for the draining process.

*   **Parameters:**
    *   `workerId` (`String`): The unique identifier for this worker instance, injected via the `cloudbalancer.worker.id` property (defaults to `worker-1`).
    *   `draining` (`AtomicBoolean`): A shared state object used to signal that the worker should begin its draining sequence.

### `onCommand` (Method)
A Kafka listener method annotated with `@KafkaListener`. It consumes JSON-serialized messages from the `workers.commands` topic.

*   **Parameters:**
    *   `message` (`String`): The raw JSON string representing a `WorkerCommand`.
*   **Behavior:**
    1.  Deserializes the message into a `WorkerCommand` object.
    2.  Validates if the command `workerId` matches the current instance's `workerId`.
    3.  If the command is an instance of `DrainCommand`, it updates the `draining` state to `true`.
    4.  Logs errors if deserialization or processing fails.

## Dependencies

*   **Spring Kafka**: Used for the `@KafkaListener` infrastructure.
*   **Jackson (via `JsonUtil`)**: Used for JSON deserialization of command payloads.
*   **`com.cloudbalancer.common.model`**: Provides the `WorkerCommand` and `DrainCommand` data models.
*   **`java.util.concurrent.atomic.AtomicBoolean`**: Used for thread-safe state management of the draining status.

## Usage Notes

*   **Topic Subscription**: The listener subscribes to the `workers.commands` Kafka topic. Ensure that the Kafka broker is reachable and the topic exists.
*   **Filtering**: The listener uses the `cloudbalancer.worker.id` property for both the Kafka `groupId` and the internal filtering logic. If multiple workers share the same ID, they will all receive and process the same commands.
*   **Draining Logic**: When a `DrainCommand` is received, the `draining` flag is set to `true`. Other components in the worker application should monitor this `AtomicBoolean` to initiate graceful shutdown procedures (e.g., stop accepting new tasks, finish current tasks, then exit).
*   **Error Handling**: The method includes a `try-catch` block to prevent malformed messages from crashing the Kafka consumer thread, logging the failure instead.
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/kafka/TaskEventsListener.java

## Overview

The `TaskEventsListener` is a Spring `@Component` responsible for processing task-related events from a Kafka message broker. It acts as an event consumer within the `metrics-aggregator` service, specifically monitoring the `tasks.events` topic to capture and persist task completion metrics.

This component bridges the gap between raw Kafka event streams and the persistent storage layer, ensuring that task lifecycle data is recorded for analytical and reporting purposes.

## Public API

### `TaskEventsListener` (Constructor)
*   **Signature**: `public TaskEventsListener(TaskMetricsRepository repository)`
*   **Description**: Initializes the listener with the required `TaskMetricsRepository` for database interactions.

### `onTaskEvent` (Method)
*   **Signature**: `@KafkaListener(topics = "tasks.events", groupId = "metrics-aggregator-group") public void onTaskEvent(String message)`
*   **Description**: The primary event handler triggered by incoming Kafka messages.
*   **Parameters**:
    *   `message` (String): The raw JSON string representing a `CloudBalancerEvent`.
*   **Functionality**:
    1.  Deserializes the JSON message into a `CloudBalancerEvent` object.
    2.  Filters for `TaskCompletedEvent` instances.
    3.  Retrieves existing metrics for the specific `taskId` or initializes a new `TaskMetricsRecord`.
    4.  Updates the record with completion timestamps and persists the changes to the repository.
    5.  Logs errors if JSON deserialization fails.

## Dependencies

*   **`com.cloudbalancer.common.event`**: Provides the event models (`CloudBalancerEvent`, `TaskCompletedEvent`).
*   **`com.cloudbalancer.common.util`**: Provides `JsonUtil` for object mapping.
*   **`com.cloudbalancer.metrics.persistence`**: Provides the repository (`TaskMetricsRepository`) and data model (`TaskMetricsRecord`).
*   **`org.springframework.kafka`**: Provides the `@KafkaListener` annotation for event-driven architecture.
*   **`com.fasterxml.jackson`**: Used for JSON processing.
*   **`org.slf4j`**: Used for logging event processing status and errors.

## Usage Notes

*   **Kafka Configuration**: This listener is configured to consume from the `tasks.events` topic using the consumer group `metrics-aggregator-group`. Ensure the Kafka broker is accessible and the topic is properly provisioned.
*   **Filtering**: The listener currently only processes `TaskCompletedEvent`. Other event types published to the `tasks.events` topic are silently ignored.
*   **Error Handling**: If a message cannot be parsed due to malformed JSON, the exception is caught and logged at the `ERROR` level, but the application continues to process subsequent messages.
*   **Idempotency**: The implementation uses `repository.findById(...)` to check for existing records, allowing for updates to existing metrics if multiple completion events for the same `taskId` are received.
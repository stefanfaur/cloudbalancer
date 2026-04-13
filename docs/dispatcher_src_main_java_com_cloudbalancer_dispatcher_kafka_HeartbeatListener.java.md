# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/HeartbeatListener.java

## Overview

`HeartbeatListener` is a Spring-managed component within the `dispatcher` module responsible for consuming and processing worker heartbeat events from Apache Kafka. It serves as the entry point for monitoring worker availability, deserializing incoming JSON heartbeat messages, and delegating the state update to the `HeartbeatTracker` service.

## Public API

### `HeartbeatListener(HeartbeatTracker heartbeatTracker)`
Constructs a new `HeartbeatListener` with the required `HeartbeatTracker` dependency.

### `onHeartbeat(String message)`
A Kafka listener method annotated with `@KafkaListener`. It consumes messages from the `workers.heartbeat` topic.
- **Parameters**: `String message` (A JSON-encoded string representing a `WorkerHeartbeatEvent`).
- **Functionality**: Deserializes the JSON payload into a `WorkerHeartbeatEvent` object and invokes `heartbeatTracker.onHeartbeat()` to record the worker's status.
- **Error Handling**: Catches and logs exceptions during deserialization or processing to prevent the Kafka consumer from crashing.

## Dependencies

- `com.cloudbalancer.common.event.WorkerHeartbeatEvent`: Data model for heartbeat events.
- `com.cloudbalancer.common.util.JsonUtil`: Utility for JSON deserialization.
- `com.cloudbalancer.dispatcher.service.HeartbeatTracker`: Service responsible for tracking and managing worker heartbeat state.
- `org.springframework.kafka.annotation.KafkaListener`: Spring Kafka annotation for message consumption.
- `org.springframework.stereotype.Component`: Spring stereotype for dependency injection.
- `org.slf4j.Logger/LoggerFactory`: Logging framework.

## Usage Notes

- **Kafka Configuration**: This component expects a Kafka topic named `workers.heartbeat` to be available. It uses the consumer group ID `dispatcher`.
- **Integration**: This listener is part of the dispatcher's monitoring infrastructure. It relies on the `HeartbeatTracker` to maintain the actual state of workers in the system.
- **Logging**: The component uses `DEBUG` level logging for successful heartbeat processing and `ERROR` level logging for failures (e.g., malformed JSON or service errors).
- **Lifecycle**: As a `@Component`, it is automatically detected and initialized by Spring's component scanning during application startup.
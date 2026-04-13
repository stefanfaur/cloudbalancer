# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/kafka/HeartbeatMetricsListener.java

## Overview

`HeartbeatMetricsListener` is a Spring-managed component responsible for consuming worker heartbeat events from the Apache Kafka message broker. It acts as a bridge between the distributed worker agents and the metrics persistence layer, ensuring that incoming heartbeat signals are parsed, validated, and recorded in the system's database for further monitoring and analysis.

## Public API

### `HeartbeatMetricsListener` (Constructor)
*   **Signature**: `public HeartbeatMetricsListener(WorkerHeartbeatRepository repository)`
*   **Description**: Initializes the listener with the required `WorkerHeartbeatRepository` for persistence operations.

### `onHeartbeat`
*   **Signature**: `public void onHeartbeat(String message)`
*   **Description**: A Kafka listener method annotated with `@KafkaListener`. It consumes raw JSON strings from the `workers.heartbeat` topic, deserializes them into `WorkerHeartbeatEvent` objects, and maps them to `WorkerHeartbeatRecord` entities before saving them to the repository.

## Dependencies

*   **`com.cloudbalancer.common.event.WorkerHeartbeatEvent`**: Data transfer object representing the heartbeat event structure.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Utility for JSON serialization/deserialization.
*   **`com.cloudbalancer.metrics.persistence.WorkerHeartbeatRecord`**: The database entity representing a stored heartbeat.
*   **`com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository`**: Data access object for persisting heartbeat records.
*   **`org.springframework.kafka.annotation.KafkaListener`**: Spring Kafka annotation for message consumption.
*   **`org.springframework.stereotype.Component`**: Marks the class as a Spring-managed bean.
*   **`com.fasterxml.jackson.core.JsonProcessingException`**: Exception handling for JSON parsing errors.
*   **`org.slf4j.Logger` / `LoggerFactory`**: Logging framework for monitoring and error reporting.

## Usage Notes

*   **Kafka Configuration**: This component listens to the `workers.heartbeat` topic. Ensure the Kafka broker is reachable and the `metrics-aggregator-group` consumer group is configured correctly in the application properties.
*   **Error Handling**: The `onHeartbeat` method includes a `try-catch` block to handle `JsonProcessingException`. If a message fails to deserialize, the error is logged, but the listener does not crash, allowing the application to continue processing subsequent messages.
*   **Logging**: The component uses `DEBUG` level logging to track successful heartbeat storage and `ERROR` level logging for deserialization failures.
*   **Integration**: This class is designed to work in conjunction with the `AgentHeartbeatPublisher` (found in the `worker-agent` module), which is responsible for producing the messages that this listener consumes.
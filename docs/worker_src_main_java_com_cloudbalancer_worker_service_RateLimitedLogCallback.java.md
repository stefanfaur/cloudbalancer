# File: worker/src/main/java/com/cloudbalancer/worker/service/RateLimitedLogCallback.java

## Overview

`RateLimitedLogCallback` is a service implementation of the `LogCallback` interface designed to handle log stream processing for cloud tasks. It provides a mechanism to buffer log messages and throttle their transmission to a Kafka topic, preventing network congestion and excessive message volume during high-frequency logging events.

The class ensures that logs are grouped and sent at a configurable minimum interval, balancing real-time observability with system throughput requirements.

## Public API

### `RateLimitedLogCallback(UUID taskId, KafkaTemplate<String, String> kafkaTemplate, long minIntervalMs)`
Constructs a new callback instance.
*   **taskId**: The unique identifier for the task generating the logs.
*   **kafkaTemplate**: The Spring Kafka template used for publishing log messages.
*   **minIntervalMs**: The minimum time (in milliseconds) that must elapse between consecutive Kafka flushes.

### `onLogLine(String line, boolean isStderr, Instant timestamp)`
Processes an incoming log line. Adds the log to an internal buffer and triggers a `flush()` if the `minIntervalMs` threshold has been met.

### `flush()`
Serializes all buffered log messages to JSON and publishes them to the `tasks.logs` Kafka topic. Clears the buffer and resets the `lastSendTime` upon successful processing.

## Dependencies

*   **com.cloudbalancer.common.executor.LogCallback**: Interface defining the log processing contract.
*   **com.cloudbalancer.common.executor.LogMessage**: Data model representing a single log entry.
*   **com.cloudbalancer.common.util.JsonUtil**: Utility for JSON serialization.
*   **org.springframework.kafka.core.KafkaTemplate**: Spring's abstraction for Kafka message production.
*   **java.util.UUID**: Used for task identification.
*   **java.util.List/ArrayList**: Used for internal log buffering.

## Usage Notes

*   **Thread Safety**: All methods (`onLogLine` and `flush`) are `synchronized` to ensure thread safety when multiple threads attempt to log messages for the same task simultaneously.
*   **Error Handling**: The `flush` method catches exceptions during JSON serialization or Kafka transmission to prevent log processing failures from crashing the worker thread. Failures are logged at the `DEBUG` level.
*   **Kafka Topic**: Logs are hardcoded to be sent to the `tasks.logs` topic.
*   **Performance**: The `minIntervalMs` parameter should be tuned based on the expected log volume of the tasks to avoid memory pressure from the `buffer` list if logs are generated faster than they can be flushed.
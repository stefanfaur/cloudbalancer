# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/LogStreamListener.java

## Overview

`LogStreamListener` is a Spring `@Component` that serves as the bridge between the Apache Kafka messaging infrastructure and the WebSocket-based log streaming service. It consumes log messages from the `tasks.logs` Kafka topic and forwards them to the appropriate connected clients via the `LogStreamWebSocketHandler`.

## Public API

### `LogStreamListener` (Constructor)
```java
public LogStreamListener(LogStreamWebSocketHandler webSocketHandler)
```
Initializes the listener with the required `LogStreamWebSocketHandler` dependency to facilitate message broadcasting.

### `onLogMessage` (Method)
```java
@KafkaListener(topics = "tasks.logs", groupId = "log-stream-consumer")
public void onLogMessage(String message)
```
An event-driven listener method triggered by incoming Kafka messages. It performs the following steps:
1. Deserializes the raw Kafka message string into a `LogMessage` object.
2. Re-serializes the object to ensure consistent JSON formatting.
3. Invokes `webSocketHandler.broadcast()` to push the log data to the specific task's subscribers.

## Dependencies

- `com.cloudbalancer.common.executor.LogMessage`: Data model for log entries.
- `com.cloudbalancer.common.util.JsonUtil`: Utility for JSON serialization and deserialization.
- `com.cloudbalancer.dispatcher.websocket.LogStreamWebSocketHandler`: Service responsible for managing active WebSocket sessions and broadcasting messages.
- `org.springframework.kafka.annotation.KafkaListener`: Spring Kafka annotation for message consumption.
- `org.slf4j.Logger`: Logging framework for error reporting.

## Usage Notes

- **Kafka Configuration**: This component expects a Kafka broker to be configured in the environment. It specifically listens to the `tasks.logs` topic using the `log-stream-consumer` group ID.
- **Error Handling**: The `onLogMessage` method includes a `try-catch` block to handle potential JSON parsing errors or broadcasting failures. Errors are logged at the `ERROR` level, ensuring that a single malformed message does not crash the listener.
- **Integration**: This class is intended to be used within the `dispatcher` module to facilitate real-time observability of cloud tasks. Ensure that the `LogStreamWebSocketHandler` is correctly initialized and available in the Spring application context before this listener starts consuming messages.
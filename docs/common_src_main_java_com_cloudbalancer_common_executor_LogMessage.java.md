# File: common/src/main/java/com/cloudbalancer/common/executor/LogMessage.java

## Overview

`LogMessage.java` is a lightweight data carrier class defined as a Java `record`. It is designed to encapsulate log entries generated during the execution of tasks within the `cloudbalancer` system. By utilizing a record, it provides an immutable, thread-safe structure for passing log data between components, ensuring that each log entry is associated with a specific task, content, stream type, and temporal metadata.

## Public API

### `LogMessage` Record

```java
public record LogMessage(UUID taskId, String line, boolean stderr, Instant timestamp) {}
```

#### Components
*   **`taskId` (UUID)**: The unique identifier of the task that generated this log entry.
*   **`line` (String)**: The actual text content of the log message.
*   **`stderr` (boolean)**: A flag indicating whether the message originated from the standard error stream (`true`) or the standard output stream (`false`).
*   **`timestamp` (Instant)**: The precise point in time when the log entry was recorded.

## Dependencies

*   `java.time.Instant`: Used to provide high-precision timestamps for log events.
*   `java.util.UUID`: Used to uniquely identify the task associated with the log message.

## Usage Notes

*   **Immutability**: As a Java record, all fields are final. Once a `LogMessage` is instantiated, its contents cannot be modified.
*   **Serialization**: Being a standard record, this class is well-suited for serialization frameworks (e.g., Jackson, Gson) commonly used in distributed systems for transmitting logs over the network.
*   **Implementation**: This class is intended to be used as a DTO (Data Transfer Object) for log aggregation and processing pipelines within the `com.cloudbalancer.common.executor` package.
*   **Primary Maintainer**: sfaur
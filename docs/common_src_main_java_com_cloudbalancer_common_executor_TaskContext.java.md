# File: common/src/main/java/com/cloudbalancer/common/executor/TaskContext.java

## Overview

`TaskContext` is an immutable data carrier used within the `com.cloudbalancer.common.executor` package to encapsulate the runtime environment and identity of a specific task. It serves as a container for the unique task identifier, the designated working directory on the filesystem, and an optional logging callback.

## Public API

### Constructors

#### `TaskContext(UUID taskId, Path workingDirectory)`
Creates a new `TaskContext` instance with a null `LogCallback`.

#### `TaskContext(UUID taskId, Path workingDirectory, LogCallback logCallback)`
Creates a new `TaskContext` instance with all fields initialized.

### Fields

*   **`taskId`**: A `UUID` representing the unique identifier for the task.
*   **`workingDirectory`**: A `Path` object pointing to the local directory where the task should perform its operations.
*   **`logCallback`**: An optional `LogCallback` interface used to handle task-related logging events.

## Dependencies

*   `java.nio.file.Path`: Used for representing filesystem paths for the task's working directory.
*   `java.util.UUID`: Used for generating and storing unique task identifiers.

## Usage Notes

*   **Immutability**: As a Java `record`, `TaskContext` is immutable. Once initialized, its fields cannot be modified.
*   **Logging**: The `logCallback` field is nullable. Consumers should implement null-checks before invoking the callback to prevent `NullPointerException` during task execution.
*   **Context Passing**: This class is intended to be passed through the executor pipeline to provide workers with the necessary metadata to isolate task execution environments.
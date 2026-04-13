# File: common/src/main/java/com/cloudbalancer/common/agent/AgentCommand.java

## Overview

The `AgentCommand` interface defines the contract for control signals sent to cloud agents within the CloudBalancer infrastructure. It is a `sealed` interface that leverages Jackson polymorphic deserialization to handle various command types, ensuring type safety and structured communication between the dispatcher and remote agents.

## Public API

### `AgentCommand` (Interface)
The base interface for all agent-related commands.

*   **`String agentId()`**: Returns the unique identifier of the agent to which the command is directed.

### Implementations

#### `StartWorkerCommand`
A record representing a request to initialize a new worker instance.
*   **Fields**:
    *   `String agentId`: Target agent ID.
    *   `String workerId`: Unique identifier for the worker to be started.
    *   `int cpuCores`: Allocated CPU cores.
    *   `int memoryMB`: Allocated memory in megabytes.
    *   `int diskMB`: Allocated disk space in megabytes.
    *   `Set<ExecutorType> supportedExecutors`: Set of executor types supported by this worker.
    *   `Set<String> tags`: Metadata tags for the worker.
    *   `Map<String, String> environment`: Environment variables for the worker process.

#### `StopWorkerCommand`
A record representing a request to terminate or drain a worker instance.
*   **Fields**:
    *   `String agentId`: Target agent ID.
    *   `String workerId`: Unique identifier for the worker to be stopped.
    *   `boolean drain`: If true, instructs the worker to finish current tasks before stopping.
    *   `int drainTimeSeconds`: The grace period in seconds allowed for draining.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Used to define supported execution environments.
*   `com.fasterxml.jackson.annotation.JsonSubTypes` / `JsonTypeInfo`: Used for polymorphic JSON serialization/deserialization.
*   `java.util.Map` / `java.util.Set`: Standard Java collections for command metadata.

## Usage Notes

*   **Polymorphism**: The interface uses the `commandType` property in JSON to determine the concrete implementation class during deserialization. Ensure any new command types are registered in the `@JsonSubTypes` annotation.
*   **Sealed Interface**: `AgentCommand` is `sealed`, meaning only the permitted implementations (`StartWorkerCommand`, `StopWorkerCommand`) can extend it. This ensures a closed set of commands for the agent control plane.
*   **Immutability**: Both implementations are defined as Java `records`, providing built-in immutability, which is ideal for command-pattern messaging.
# File: common/src/main/java/com/cloudbalancer/common/model/DrainCommand.java

## Overview

The `DrainCommand` class is a Java `record` that represents a specific control signal sent to worker nodes within the CloudBalancer infrastructure. It instructs a designated worker to gracefully cease accepting new tasks and prepare for shutdown or maintenance within a specified timeframe.

This class implements the `WorkerCommand` interface, ensuring compatibility with the broader command-processing pipeline used across the system.

## Public API

### `DrainCommand` (Constructor)
Creates a new instance of the command.

*   **Parameters**:
    *   `String workerId`: The unique identifier of the worker node targeted by this command.
    *   `int drainTimeSeconds`: The duration in seconds allowed for the worker to complete active tasks before termination.
    *   `Instant timestamp`: The time at which the command was generated.

### `commandType()`
Returns the string identifier for this command type.

*   **Returns**: `String` - Always returns `"DRAIN"`.

## Dependencies

*   `java.time.Instant`: Used to record the precise temporal context of the command issuance.
*   `com.cloudbalancer.common.model.WorkerCommand`: The interface implemented by this record to facilitate polymorphic command handling.

## Usage Notes

*   **Immutability**: As a Java `record`, instances of `DrainCommand` are immutable. Once created, the `workerId`, `drainTimeSeconds`, and `timestamp` cannot be modified.
*   **Integration**: This command is intended to be serialized and transmitted to worker nodes. Ensure that the JSON or binary serialization framework used by the system is configured to handle Java records.
*   **Processing**: When a worker node receives a `DrainCommand`, it should initiate a graceful shutdown sequence, respecting the `drainTimeSeconds` threshold to minimize impact on ongoing processing tasks.
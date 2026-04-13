# File: common/src/main/java/com/cloudbalancer/common/model/WorkerCommand.java

## Overview

The `WorkerCommand` interface defines the contract for control signals sent to worker nodes within the CloudBalancer infrastructure. It serves as a polymorphic base type for various command implementations, enabling the system to serialize and deserialize specific command types using Jackson annotations.

This interface is `sealed`, meaning only explicitly permitted classes (such as `DrainCommand`) can implement it, ensuring type safety and controlled extensibility for worker-side operations.

## Public API

### `WorkerCommand` (Interface)

The base interface for all worker-related commands.

#### Methods

*   **`String workerId()`**
    Returns the unique identifier of the worker node to which the command is directed.

*   **`String commandType()`**
    Returns the string representation of the command type (e.g., "DRAIN"). This is used by the Jackson deserializer to map the JSON payload to the correct implementation class.

## Dependencies

*   `com.fasterxml.jackson.annotation.JsonSubTypes`: Used to define the mapping between command type names and their corresponding implementation classes.
*   `com.fasterxml.jackson.annotation.JsonTypeInfo`: Configures the polymorphic serialization/deserialization behavior, specifically using the `commandType` field as the type identifier.

## Usage Notes

*   **Polymorphic Deserialization**: The interface is annotated with `@JsonTypeInfo`, which expects a `commandType` property in the JSON payload to determine the concrete class instance.
*   **Sealed Hierarchy**: As a `sealed` interface, any new command types must be added to the `permits` clause of the `WorkerCommand` definition and registered in the `@JsonSubTypes` annotation to be recognized by the serialization framework.
*   **Implementation**: Currently, the only permitted implementation is `DrainCommand`. Ensure that any future implementations are added to the `permits` list to maintain compatibility with the existing infrastructure.
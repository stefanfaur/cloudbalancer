# File: common/src/main/java/com/cloudbalancer/common/executor/ExecutionHandle.java

## Overview

The `ExecutionHandle` class is a lightweight, immutable data carrier defined as a Java `record`. It serves as a unique identifier wrapper for asynchronous or long-running tasks within the `com.cloudbalancer.common.executor` package. By encapsulating the `handleId`, it provides a type-safe mechanism for tracking and referencing specific execution contexts across the system.

## Public API

### Constructors
*   `ExecutionHandle(String handleId)`: Initializes a new handle with the specified unique identifier.

### Methods
*   `String handleId()`: Returns the unique identifier associated with this execution handle.
*   `boolean equals(Object o)`: Standard equality check based on the `handleId` field.
*   `int hashCode()`: Returns the hash code derived from the `handleId`.
*   `String toString()`: Returns a string representation of the record.

## Dependencies

This class has no external dependencies and relies solely on the Java standard library (JDK 14+ for the `record` feature).

## Usage Notes

*   **Immutability**: As a `record`, `ExecutionHandle` is immutable. Once created, the `handleId` cannot be modified.
*   **Identity**: This class is intended to be used as a key or reference token. Ensure that the `handleId` passed during instantiation is unique within the scope of the executor service to prevent collision or tracking errors.
*   **Primary Maintainer**: sfaur
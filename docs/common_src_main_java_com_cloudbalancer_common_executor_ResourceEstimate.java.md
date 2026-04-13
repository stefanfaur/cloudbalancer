# File: common/src/main/java/com/cloudbalancer/common/executor/ResourceEstimate.java

## Overview

The `ResourceEstimate` class is a Java `record` used to encapsulate the projected resource requirements for a task or process within the `cloudbalancer` system. It serves as a lightweight data carrier for planning and scheduling operations, providing a structured way to represent CPU, memory, and temporal requirements.

## Public API

### Constructors
*   `ResourceEstimate(int estimatedCpuCores, int estimatedMemoryMB, long estimatedDurationMs)`: Constructs a new `ResourceEstimate` instance with the specified resource parameters.

### Accessors
*   `int estimatedCpuCores()`: Returns the number of CPU cores estimated for the task.
*   `int estimatedMemoryMB()`: Returns the estimated memory requirement in megabytes.
*   `long estimatedDurationMs()`: Returns the estimated execution duration in milliseconds.

### Standard Methods
As a Java `record`, this class automatically provides implementations for:
*   `equals(Object o)`: Compares two `ResourceEstimate` objects for equality based on their fields.
*   `hashCode()`: Generates a hash code based on the record components.
*   `toString()`: Returns a string representation of the record.

## Dependencies

This class is a standalone POJO (Plain Old Java Object) and does not depend on any external libraries or internal project classes outside of the standard Java SE library.

## Usage Notes

*   **Immutability**: Being a `record`, instances of `ResourceEstimate` are immutable. Once created, the values cannot be modified.
*   **Use Case**: This class is intended for use by the executor and scheduler components to make informed decisions about resource allocation and task placement within the cloud environment.
*   **Validation**: Note that this class does not perform internal validation (e.g., checking for negative values). It is expected that the producing service validates these inputs before instantiation.
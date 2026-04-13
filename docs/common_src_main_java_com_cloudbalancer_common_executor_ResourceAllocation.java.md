# File: common/src/main/java/com/cloudbalancer/common/executor/ResourceAllocation.java

## Overview

The `ResourceAllocation` class is a Java `record` located in the `com.cloudbalancer.common.executor` package. It serves as an immutable data carrier designed to encapsulate the hardware resource requirements or assignments for a cloud-based task or container. By utilizing the `record` feature, it provides a concise, boilerplate-free way to represent resource constraints, including CPU, memory, and disk specifications.

## Public API

### Constructors
*   `ResourceAllocation(int cpuCores, int memoryMB, int diskMB)`: Constructs a new `ResourceAllocation` instance with the specified resource values.

### Accessors
*   `int cpuCores()`: Returns the number of CPU cores allocated.
*   `int memoryMB()`: Returns the memory allocation in megabytes.
*   `int diskMB()`: Returns the disk storage allocation in megabytes.

### Standard Methods
As a Java `record`, this class automatically provides implementations for:
*   `equals(Object o)`: Compares two `ResourceAllocation` objects for structural equality.
*   `hashCode()`: Generates a hash code based on the record components.
*   `toString()`: Returns a string representation of the resource allocation.

## Dependencies

This class is a standalone data structure and does not depend on any external libraries or internal project modules outside of the standard Java SE library.

## Usage Notes

*   **Immutability**: Since this is a `record`, instances are immutable. Once a `ResourceAllocation` object is created, its values cannot be modified.
*   **Use Case**: This class is intended to be used as a DTO (Data Transfer Object) when passing resource requirements between the executor service and the underlying cloud infrastructure providers.
*   **Validation**: Note that this class does not perform range validation (e.g., checking for negative values). It is recommended that calling services validate the input parameters before instantiating the record to ensure resource values are logical and within system limits.
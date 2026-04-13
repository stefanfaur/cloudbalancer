# File: common/src/main/java/com/cloudbalancer/common/model/ResourceProfile.java

## Overview

The `ResourceProfile` class is a Java `record` located in the `com.cloudbalancer.common.model` package. It serves as a data carrier for defining the hardware and operational requirements of a specific task or workload within the cloud balancer system. By utilizing a record, it provides an immutable, concise representation of resource constraints, facilitating predictable data transfer across the application.

## Public API

### Constructors
*   `ResourceProfile(int cpuCores, int memoryMB, int diskMB, boolean gpuRequired, int estimatedDurationSeconds, boolean networkAccessRequired)`: Constructs a new `ResourceProfile` with the specified resource constraints.

### Accessors
*   `int cpuCores()`: Returns the number of CPU cores required.
*   `int memoryMB()`: Returns the required memory capacity in megabytes.
*   `int diskMB()`: Returns the required disk storage capacity in megabytes.
*   `boolean gpuRequired()`: Returns true if a GPU is required for the workload.
*   `int estimatedDurationSeconds()`: Returns the estimated execution time in seconds.
*   `boolean networkAccessRequired()`: Returns true if the workload requires external network access.

## Dependencies

This class is a standard Java record and does not depend on any external libraries or internal project modules outside of the standard Java Development Kit (JDK).

## Usage Notes

*   **Immutability**: As a Java record, all fields are final. Once a `ResourceProfile` is instantiated, its values cannot be modified.
*   **Equality**: The record automatically implements `equals()`, `hashCode()`, and `toString()` based on all defined fields. Two `ResourceProfile` instances are considered equal if all their constituent fields match.
*   **Primary Use Case**: This class is intended to be used by scheduling and balancing algorithms to evaluate whether a specific node or cluster can accommodate a requested workload based on the provided hardware metrics.
*   **Maintainer**: Primary maintenance is handled by **sfaur**.
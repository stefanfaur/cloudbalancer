# File: common/src/main/java/com/cloudbalancer/common/runtime/WorkerConfig.java

## Overview

The `WorkerConfig` class is a Java `record` that serves as a data transfer object (DTO) representing the configuration profile of a worker node within the CloudBalancer infrastructure. It encapsulates hardware specifications, supported execution capabilities, and metadata tags required for the orchestration and scheduling of tasks across the cluster.

## Public API

### `WorkerConfig` (Record)

The class is defined as a `record`, providing immutable storage for worker configuration data.

**Fields:**
*   `String workerId`: A unique identifier for the worker node.
*   `Set<ExecutorType> supportedExecutors`: A set of `ExecutorType` enums defining the types of tasks or execution environments the worker is capable of handling.
*   `int cpuCores`: The total number of CPU cores allocated or available on the worker.
*   `int memoryMB`: The total memory capacity of the worker in megabytes.
*   `int diskMB`: The total disk storage capacity of the worker in megabytes.
*   `Set<String> tags`: A set of arbitrary string tags used for filtering, grouping, or affinity-based scheduling.

**Methods:**
As a standard Java record, it automatically provides:
*   `workerId()`: Accessor for the worker ID.
*   `supportedExecutors()`: Accessor for the set of supported executors.
*   `cpuCores()`: Accessor for the CPU core count.
*   `memoryMB()`: Accessor for the memory capacity.
*   `diskMB()`: Accessor for the disk capacity.
*   `tags()`: Accessor for the worker tags.
*   `equals(Object o)`, `hashCode()`, and `toString()`: Standard object overrides.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Used to define the capabilities of the worker.
*   `java.util.Set`: Used for managing collections of supported executors and metadata tags.

## Usage Notes

*   **Immutability**: Because `WorkerConfig` is a `record`, instances are immutable. Any changes to a worker's configuration require the creation of a new instance.
*   **Scheduling**: The `supportedExecutors` and `tags` fields are primarily intended for use by the cluster scheduler to determine if a specific worker is eligible to execute a given task.
*   **Resource Management**: The integer fields (`cpuCores`, `memoryMB`, `diskMB`) should be populated with the effective capacity of the worker to ensure accurate resource accounting during task placement.
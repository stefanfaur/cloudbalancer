# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/FilterTestHelper.java

## Overview

`FilterTestHelper` is a utility class designed to facilitate unit testing for scheduling filters and scorers within the `dispatcher` module. It provides a set of static factory methods to generate `TaskRecord` and `WorkerRecord` objects with specific configurations, eliminating the need for complex object instantiation or Spring context initialization during tests.

## Public API

### Task Creation
*   **`anyTask()`**: Returns a default `TaskRecord` using `ExecutorType.SIMULATED`.
*   **`taskWithExecutor(ExecutorType type)`**: Creates a `TaskRecord` with a specific executor type and default resource/constraint settings.
*   **`taskWithResources(int cpu, int memMb, int diskMb)`**: Creates a `TaskRecord` with custom resource requirements.
*   **`taskWithConstraints(Set<String> requiredTags, Set<String> blacklist, Set<String> whitelist)`**: Creates a `TaskRecord` with specific scheduling constraints.
*   **`taskWith(...)`**: The base factory method that constructs a `TaskRecord` with full control over executor type, resources, constraints, and priority.

### Worker Creation
*   **`workerRecord(String id)`**: Creates a healthy `WorkerRecord` with default capabilities.
*   **`workerRecord(String id, WorkerHealthState state)`**: Creates a `WorkerRecord` with a specific health state.
*   **`workerWithExecutors(String id, Set<ExecutorType> executors)`**: Creates a `WorkerRecord` supporting a specific set of executor types.
*   **`workerWithCapacity(String id, int totalCpu, int totalMem, int totalDisk, int allocCpu, int allocMem, int allocDisk)`**: Creates a `WorkerRecord` with defined total capacity and current resource allocations.
*   **`workerWithTags(String id, Set<String> tags)`**: Creates a `WorkerRecord` associated with a specific set of metadata tags.

## Dependencies

*   `com.cloudbalancer.common.model.*`: Provides core domain models like `ExecutorType`, `ResourceProfile`, `TaskConstraints`, and `WorkerCapabilities`.
*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: The persistence model for tasks.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: The persistence model for workers.

## Usage Notes

*   **No Spring Context**: This class is designed to be lightweight and does not require a Spring context, making it ideal for fast-running unit tests.
*   **State Management**: Methods like `taskWith` automatically transition created tasks to `TaskState.QUEUED`, ensuring they are ready for filter evaluation immediately upon creation.
*   **Default Values**: When using specialized methods (e.g., `workerWithTags`), default values are applied to unspecified fields (e.g., default resource profiles) to maintain test consistency.
*   **Extensibility**: If a test requires a specific combination of attributes not covered by the helper methods, use the `taskWith` method directly to define the `TaskDescriptor`.
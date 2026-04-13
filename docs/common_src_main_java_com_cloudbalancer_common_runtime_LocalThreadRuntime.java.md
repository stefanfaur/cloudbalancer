# File: common/src/main/java/com/cloudbalancer/common/runtime/LocalThreadRuntime.java

## Overview

`LocalThreadRuntime` is an implementation of the `NodeRuntime` interface designed for managing worker lifecycles within a local JVM environment. It provides a thread-safe, in-memory registry for tracking worker states, capabilities, and resource profiles. This implementation is primarily used for testing, local development, or lightweight deployments where distributed orchestration is not required.

## Public API

### `startWorker(WorkerConfig config)`
Registers and initializes a new worker based on the provided `WorkerConfig`. It constructs `WorkerCapabilities` and initial `WorkerMetrics`, storing the resulting `WorkerInfo` in the internal registry.
*   **Returns**: `boolean` - Returns `true` if the worker was successfully registered.

### `stopWorker(String workerId)`
Removes the specified worker from the registry, effectively terminating its tracking within the local runtime.

### `getWorkerInfo(String workerId)`
Retrieves the current `WorkerInfo` snapshot for a specific worker.
*   **Returns**: `WorkerInfo` object if found, otherwise `null`.

### `listWorkers()`
Returns an immutable list of all workers currently registered in the local runtime.
*   **Returns**: `List<WorkerInfo>` containing the current state of all active workers.

## Dependencies

*   `com.cloudbalancer.common.model.*`: Provides the core data models (`WorkerConfig`, `WorkerInfo`, `WorkerCapabilities`, `WorkerMetrics`, `WorkerHealthState`) required for state management.
*   `java.util.concurrent.ConcurrentHashMap`: Used to ensure thread-safe access to the worker registry.
*   `java.time.Instant`: Used for timestamping worker registration and metric updates.

## Usage Notes

*   **Thread Safety**: This class uses `ConcurrentHashMap`, making it safe for concurrent access by multiple threads when starting, stopping, or querying workers.
*   **Volatile State**: Because the registry is held in memory, all worker information is lost upon JVM termination. It is not suitable for production environments requiring persistence or cross-node visibility.
*   **Resource Modeling**: The `startWorker` method automatically maps `WorkerConfig` parameters (CPU, memory, disk) into a `ResourceProfile`, which is then encapsulated within the `WorkerInfo`.
*   **Implementation**: This class implements `NodeRuntime`, ensuring compatibility with other components expecting a standard runtime interface for node management.
# File: common/src/main/java/com/cloudbalancer/common/runtime/NodeRuntime.java

## Overview

The `NodeRuntime` interface defines the abstraction layer for managing worker lifecycles within the CloudBalancer infrastructure. It provides a standardized contract for node-level operations, allowing the system to interact with various container runtimes (such as Docker or Kubernetes) without requiring implementation-specific logic in the core services.

## Public API

### Methods

*   **`boolean startWorker(WorkerConfig config)`**
    Initiates the deployment and execution of a new worker instance based on the provided configuration. Returns `true` if the worker was successfully started.

*   **`void stopWorker(String workerId)`**
    Immediately terminates the worker instance associated with the specified `workerId`.

*   **`WorkerInfo getWorkerInfo(String workerId)`**
    Retrieves the current status and metadata for a specific worker.

*   **`List<WorkerInfo> listWorkers()`**
    Returns a list of all active workers currently managed by the runtime.

*   **`void drainAndStop(String workerId, int drainTimeSeconds)`**
    Gracefully shuts down a worker by allowing it to finish processing existing tasks within the specified `drainTimeSeconds` before termination. 
    *Note: This method provides a default no-op implementation; concrete runtime implementations (e.g., `DockerRuntime`) should override this to handle specific graceful shutdown signals.*

## Dependencies

*   `com.cloudbalancer.common.model.WorkerInfo`: Used for representing the state and metadata of worker nodes.
*   `java.util.List`: Used for collection management in worker listing operations.

## Usage Notes

*   **Implementation Strategy**: This interface is intended to be implemented by infrastructure-specific adapters. For instance, a `DockerRuntime` implementation would map these methods to Docker API calls, while a `KubernetesRuntime` would interact with the K8s API.
*   **Graceful Shutdown**: When implementing `drainAndStop`, ensure that the underlying container runtime is instructed to stop accepting new tasks (e.g., via a SIGTERM or a specific message queue signal) before the final termination command is issued.
*   **Error Handling**: Implementations should handle runtime exceptions (e.g., connection timeouts to the container engine) and ensure that the `startWorker` method returns a boolean indicating success or failure to allow the caller to handle deployment retries.
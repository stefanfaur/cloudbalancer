# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/WorkerRegistryService.java

## Overview

The `WorkerRegistryService` is a core component of the `dispatcher` module, responsible for managing the lifecycle, health status, and resource allocation of worker nodes. It acts as the primary interface between the system's persistence layer (`WorkerRepository`) and the operational logic that handles worker registration, resource tracking, and state transitions (e.g., draining, killing, or marking as dead).

**Critical Note**: This file is a **HOTSPOT** within the codebase. It exhibits high change frequency and significant complexity, serving as a central point of failure for cluster orchestration. Modifications to state transition logic or resource ledger calculations should be approached with extreme caution, as they directly impact task scheduling and system stability.

## Public API

### Lifecycle Management
*   **`registerWorker(String workerId, WorkerHealthState healthState, WorkerCapabilities capabilities)`**: Registers a new worker or updates an existing one. If a `DEAD` worker re-registers, it transitions to `RECOVERING`.
*   **`registerWorker(String workerId, WorkerHealthState healthState, WorkerCapabilities capabilities, String agentId)`**: Overloaded version that associates the worker with a specific `agentId`.
*   **`killWorker(String workerId)`**: Transitions a worker to `STOPPING`. Throws `IllegalStateException` if the worker is already `DEAD` or `STOPPING`.
*   **`drainWorker(String workerId)`**: Transitions a worker to `DRAINING`, signaling that it should no longer accept new tasks.
*   **`markDead(String workerId)`**: Forcefully sets a worker's state to `DEAD`.

### Querying
*   **`getWorker(String workerId)`**: Retrieves a single worker record.
*   **`getAvailableWorkers()`**: Returns all workers in `HEALTHY` or `RECOVERING` states.
*   **`getAllWorkers()`**: Returns the complete list of registered workers.
*   **`getAvailableWorkersByAgent(String agentId)`**: Returns available workers filtered by agent.
*   **`getWorkersByAgent(String agentId)`**: Returns all workers associated with a specific agent.

### Resource & Configuration
*   **`allocateResources(String workerId, ResourceProfile profile)`**: Updates the worker's internal resource ledger to reflect allocated capacity.
*   **`releaseResources(String workerId, ResourceProfile profile)`**: Frees resources in the worker's ledger.
*   **`updateWorkerTags(String workerId, Set<String> tags)`**: Updates the metadata tags for a worker.
*   **`rebuildResourceLedger()`**: A maintenance operation that resets all worker resource ledgers and recalculates them based on currently active tasks.

## Dependencies

*   **Persistence Layer**: `WorkerRepository`, `TaskRepository`
*   **Models**: `ResourceProfile`, `TaskState`, `WorkerCapabilities`, `WorkerHealthState`
*   **Infrastructure**: `org.slf4j.Logger` (Logging), `org.springframework.transaction.annotation.Transactional` (Transaction management)

## Usage Notes

### Resource Ledger Consistency
The `rebuildResourceLedger()` method is an expensive operation that performs a full scan of active tasks. It is intended for recovery scenarios where the in-memory resource state of workers may have drifted from the actual task assignments in the database. Avoid calling this in high-frequency loops.

### State Transitions
*   **Soft-Start**: The service automatically handles "soft-starts" for workers returning from a `DEAD` state by transitioning them to `RECOVERING`. Ensure that the scheduler logic respects the `RECOVERING` state if you wish to limit load on newly recovered nodes.
*   **Draining**: Use `drainWorker` before performing maintenance on a node. This allows the system to gracefully stop scheduling new tasks to the node while existing tasks complete.

### Example: Registering and Allocating
```java
// 1. Register a worker
workerRegistryService.registerWorker("worker-01", WorkerHealthState.HEALTHY, myCapabilities, "agent-alpha");

// 2. Allocate resources for a task
ResourceProfile profile = new ResourceProfile(4, 8192); // 4 cores, 8GB RAM
workerRegistryService.allocateResources("worker-01", profile);

// 3. Later, release resources
workerRegistryService.releaseResources("worker-01", profile);
```

### Potential Pitfalls
*   **Transactionality**: While `killWorker` is marked `@Transactional`, other methods rely on the repository's default behavior. Ensure that external callers wrap sequences of state changes in a transaction if atomicity is required.
*   **Race Conditions**: Because this is a high-concurrency service, ensure that `allocateResources` and `releaseResources` are handled in a way that prevents double-allocation if the underlying `WorkerRecord` is updated by multiple threads simultaneously.
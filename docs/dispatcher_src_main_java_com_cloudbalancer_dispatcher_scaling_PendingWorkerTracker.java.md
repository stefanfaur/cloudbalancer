# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scaling/PendingWorkerTracker.java

## Overview

`PendingWorkerTracker` is a Spring `@Component` responsible for managing the lifecycle state of workers that are currently in the process of being provisioned or initialized. It acts as an in-memory registry to track which agent is responsible for a specific worker request, allowing the system to handle asynchronous worker startup events, resolve successful deployments, or clean up failed/stale requests.

## Public API

### `markPending(String workerId, String agentId, Instant requestedAt)`
Registers a worker as "pending." This associates a specific `workerId` with an `agentId` and the timestamp of the request.

### `resolve(String workerId)`
Removes the worker from the pending registry, indicating that the worker has successfully started or is no longer in a pending state.

### `fail(String workerId)`
Removes the worker from the pending registry due to a failure in the startup process.

### `expireStale(Duration timeout)`
Removes all pending entries that were requested longer ago than the provided `timeout` duration. This is used to prevent memory leaks from abandoned worker requests.

### `pendingCount()`
Returns the total number of workers currently tracked as pending.

### `getAgentForWorker(String workerId)`
Retrieves the `agentId` associated with a specific `workerId`. Returns `null` if the worker is not currently tracked as pending.

## Dependencies

*   `java.time.Duration`
*   `java.time.Instant`
*   `java.util.concurrent.ConcurrentHashMap`
*   `org.springframework.stereotype.Component`

## Usage Notes

*   **Thread Safety**: The class uses a `ConcurrentHashMap` to store state, making it safe for concurrent access in multi-threaded environments (e.g., handling multiple asynchronous Kafka events).
*   **Memory Management**: The `expireStale` method should be invoked periodically (e.g., via a scheduled task) to ensure that workers that never received a success or failure signal do not persist indefinitely in memory.
*   **Lifecycle**: This component is intended for use within the dispatcher service to bridge the gap between requesting a worker and receiving confirmation of its operational status.
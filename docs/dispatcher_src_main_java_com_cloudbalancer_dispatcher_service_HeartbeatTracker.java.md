# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/HeartbeatTracker.java

## Overview

The `HeartbeatTracker` is a core service within the `dispatcher` module responsible for monitoring the health status of worker nodes. It maintains an in-memory registry of the last time each worker reported a heartbeat and periodically evaluates worker liveness against defined thresholds.

**Warning: Hotspot File**
This file is a high-activity component with significant complexity. It is ranked in the top 25% for both change frequency and complexity. Modifications to the liveness logic or state transition rules carry a high risk of introducing cascading failures in worker management.

## Public API

### `onHeartbeat(String workerId, Instant timestamp)`
Updates the last-seen timestamp for a specific worker. If the worker is currently in a `SUSPECT` state, it is automatically promoted back to `HEALTHY`. `STOPPING` workers are ignored to prevent accidental recovery.

### `checkLiveness()`
A `@Scheduled` task that runs at a fixed interval (default: 10 seconds). It iterates through all registered workers to perform state transitions:
*   **RECOVERING → HEALTHY**: Promoted after 60 seconds of recovery.
*   **STOPPING → DEAD**: Transitioned to `DEAD` if the stopping timeout is exceeded.
*   **HEALTHY → SUSPECT**: Transitioned if the heartbeat exceeds `suspectThresholdSeconds`.
*   **SUSPECT/HEALTHY → DEAD**: Transitioned if the heartbeat exceeds `deadThresholdSeconds`.

### `getLastSeenMap()`
Returns the internal `ConcurrentHashMap` containing the `Instant` of the last received heartbeat for each worker ID.

## Dependencies

*   **`WorkerRepository`**: Used to fetch and persist the current state of workers in the database.
*   **`WorkerFailureHandler`**: Triggered when a worker is marked `DEAD` to handle task re-queuing and failure recovery.
*   **Spring Scheduling**: Utilizes `@Scheduled` for periodic liveness checks.
*   **`WorkerRecord`**: The persistence model representing the worker node.

## Usage Notes

### Configuration
The thresholds are configurable via `application.yml` (or environment variables):
*   `cloudbalancer.dispatcher.heartbeat-suspect-threshold-seconds`: Defaults to `30`. Time after which a worker is marked `SUSPECT`.
*   `cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds`: Defaults to `60`. Time after which a worker is marked `DEAD`.
*   `cloudbalancer.dispatcher.liveness-check-interval-ms`: Defaults to `10000` (10 seconds). The frequency of the `checkLiveness` task.

### Implementation Rationale
*   **State Transitions**: The logic prioritizes explicit states (`STOPPING`, `RECOVERING`) over heartbeat-based inference. For example, a `STOPPING` worker will never be marked `DEAD` via a heartbeat timeout, only via the `stoppingStartedAt` timer.
*   **Concurrency**: The use of `ConcurrentHashMap` ensures thread-safe updates when multiple heartbeat events arrive simultaneously from the `HeartbeatListener`.
*   **Failure Handling**: When a worker is marked `DEAD`, `workerFailureHandler.onWorkerDead()` is invoked. This is a critical path; ensure that any custom implementations of this handler are idempotent, as the `checkLiveness` loop will continue to trigger this if the worker remains in the repository.

### Potential Pitfalls
*   **Memory Usage**: The `lastSeenAt` map grows with the number of unique worker IDs. In environments with high worker churn, ensure that `WorkerRepository` cleanup processes are in place to prevent memory leaks in the map.
*   **Clock Skew**: The logic relies on `Instant.now()`. Ensure that all nodes in the cluster have synchronized clocks (NTP) to prevent premature or delayed marking of workers as `DEAD`.
*   **Blocking Operations**: Since `checkLiveness` iterates over all workers in the repository, a very large number of workers could cause the scheduled task to exceed its execution window. Monitor the performance of `workerRepository.findAll()` if the cluster scales to thousands of nodes.
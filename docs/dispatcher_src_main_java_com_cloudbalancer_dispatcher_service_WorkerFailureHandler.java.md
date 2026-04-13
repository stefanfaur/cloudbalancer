# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/WorkerFailureHandler.java

## Overview

The `WorkerFailureHandler` is a core service within the `dispatcher` module responsible for maintaining system reliability when worker nodes become unavailable. It detects worker failures and orchestrates the recovery of tasks that were in progress on the failed node.

The service ensures that tasks in `ASSIGNED`, `PROVISIONING`, or `RUNNING` states are safely transitioned back to a `QUEUED` state, allowing them to be rescheduled on healthy workers without counting the failure against the task's retry limit.

## Public API

### `WorkerFailureHandler(TaskRepository, TaskService, WorkerRegistryService, EventPublisher)`
Constructs a new `WorkerFailureHandler` with the necessary dependencies for persistence, task lifecycle management, resource tracking, and event notification.

### `onWorkerDead(String workerId)`
The primary entry point for handling worker failure. 
*   **Parameters**: `workerId` (String) - The unique identifier of the worker that has been marked as dead.
*   **Functionality**:
    1.  Queries the `TaskRepository` for all tasks currently assigned to the failed worker.
    2.  Records a failure attempt for each task, explicitly marking it as a "worker-caused" failure to prevent it from exhausting the task's retry budget.
    3.  Transitions tasks through the lifecycle to `QUEUED`.
    4.  Clears the `assignedWorkerId` and generates a new `executionId`.
    5.  Releases resources associated with the worker via `WorkerRegistryService`.
    6.  Publishes a `TaskStateChangedEvent` to notify downstream systems of the state transition.

## Dependencies

*   **`TaskRepository`**: Used to fetch tasks currently assigned to the failed worker.
*   **`TaskService`**: Used to persist task state transitions and updates.
*   **`WorkerRegistryService`**: Used to release resource reservations held by the failed worker.
*   **`EventPublisher`**: Used to broadcast task state changes to the Kafka event bus.
*   **`TaskRecord`**: The domain model representing the task state and execution history.

## Usage Notes

*   **Idempotency**: This service is designed to be triggered by heartbeat monitoring or health check failures. Ensure that the calling service (e.g., `HeartbeatTracker`) implements appropriate deduplication to avoid redundant processing of the same worker failure.
*   **Resource Cleanup**: The `onWorkerDead` method assumes that the `WorkerRegistryService` can accurately map the task's resource profile back to the worker's capacity.
*   **State Transitions**: The method forces a state transition path (`ASSIGNED` -> `PROVISIONING` -> `RUNNING` -> `FAILED` -> `QUEUED`). This is required to maintain the integrity of the `TaskRecord` state machine and ensure audit logs reflect the transition correctly.
*   **Retry Logic**: Because the failure is flagged as `worker-caused` (via the `ExecutionAttempt` constructor), these failures do not increment the standard retry counter, ensuring that tasks are not prematurely aborted due to infrastructure instability.
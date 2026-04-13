# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/TaskAssignmentService.java

## Overview

`TaskAssignmentService` is a core component of the `dispatcher` module responsible for the automated orchestration of task distribution. It bridges the gap between queued tasks and available compute resources by executing a scheduling pipeline at fixed intervals.

**⚠️ HOTSPOT WARNING:** This file is identified as a high-activity hotspot (top 25% for change frequency and complexity). It is a critical path for system stability; modifications here carry a high risk of introducing scheduling regressions, race conditions in resource allocation, or event propagation failures.

## Public API

### `TaskAssignmentService` (Constructor)
Initializes the service with required infrastructure dependencies.
*   **Parameters**:
    *   `TaskService`: Manages task persistence and state retrieval.
    *   `WorkerRegistryService`: Tracks active worker nodes and resource availability.
    *   `SchedulingPipeline`: Implements the logic for matching tasks to workers.
    *   `EventPublisher`: Handles Kafka-based event propagation.
    *   `ChaosMonkeyService`: Injects controlled latency for resilience testing.

### `assignPendingTasks` (Method)
The primary execution loop, triggered by a `@Scheduled` annotation.
*   **Behavior**:
    1.  Invokes `ChaosMonkeyService` to simulate network/processing jitter.
    2.  Fetches all active workers from the `WorkerRegistryService`.
    3.  Iterates through queued tasks and attempts to match them via `SchedulingPipeline`.
    4.  Updates task state to `ASSIGNED`, reserves resources on the target worker, and publishes assignment events.
    5.  Refreshes the worker registry state after each assignment to ensure subsequent iterations in the same loop account for updated resource availability.

## Dependencies

*   **`com.cloudbalancer.common.event.TaskAssignedEvent`**: Schema for event-driven task notifications.
*   **`com.cloudbalancer.dispatcher.persistence.TaskRecord`**: The internal representation of a task's lifecycle state.
*   **`com.cloudbalancer.dispatcher.scheduling.SchedulingPipeline`**: The strategy engine for task-to-worker mapping.
*   **`com.cloudbalancer.dispatcher.kafka.EventPublisher`**: Infrastructure for cross-service communication.
*   **`com.cloudbalancer.dispatcher.service.ChaosMonkeyService`**: Resilience testing utility.

## Usage Notes

### Scheduling Configuration
The assignment interval is controlled by the Spring property `cloudbalancer.dispatcher.assignment-interval-ms`. It defaults to `1000ms`. Adjusting this value impacts the system's responsiveness to new tasks versus the overhead on the `WorkerRegistryService`.

### Resource Allocation Logic
The service performs a "greedy" assignment within each scheduled tick. Because it refreshes the worker list (`allWorkers = workerRegistry.getAllWorkers()`) after every successful assignment, it ensures that resource exhaustion on a worker is respected immediately for the next task in the queue.

### Potential Pitfalls
*   **Eventual Consistency**: The `WorkerRegistryService` and `TaskService` operate on potentially stale data if the underlying persistence layer experiences lag.
*   **Chaos Injection**: The `ChaosMonkeyService` can cause the `assignPendingTasks` method to block or throw an `InterruptedException`. This is intentional for testing, but can lead to "assignment gaps" if the latency exceeds the scheduling interval.
*   **Task State Transitions**: Ensure that any logic added to `assignPendingTasks` respects the `TaskState` machine defined in `web-dashboard/src/api/types.ts`. Improper state transitions will cause the `TaskResultListener` to fail when processing results.

### Example Workflow
1.  **Queueing**: A task is persisted in `TaskService` with a `PENDING` state.
2.  **Trigger**: The `@Scheduled` task fires.
3.  **Selection**: `SchedulingPipeline` evaluates the `ResourceProfile` of the task against available workers.
4.  **Commit**: The task is updated to `ASSIGNED`, and `EventPublisher` broadcasts the assignment to the target worker via Kafka.
5.  **Verification**: The `TaskResultListener` receives the confirmation, completing the lifecycle loop.
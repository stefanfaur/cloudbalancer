# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/TaskResultListener.java

## Overview

The `TaskResultListener` is a core component of the `dispatcher` service, acting as the primary feedback loop for task execution. It consumes JSON-serialized `TaskResult` messages from the `tasks.results` Kafka topic and updates the internal state of tasks within the system.

**Hotspot Warning**: This file is a high-activity hotspot with significant complexity. It manages critical state transitions, resource lifecycle management, and event propagation. Changes to this class carry a high risk of introducing race conditions or inconsistent task states.

## Public API

### `TaskResultListener` (Constructor)
Initializes the listener with required services and configuration.
- **Parameters**:
    - `TaskService`: Manages task persistence and state updates.
    - `WorkerRegistryService`: Manages worker resource allocation and release.
    - `EventPublisher`: Dispatches downstream events (e.g., `TaskCompletedEvent`).
    - `AutoScalerService`: Tracks task completion for cluster scaling metrics.
    - `baseDelaySeconds`: Configured via `cloudbalancer.retry.base-delay-seconds` (default: 5s) for backoff calculations.

### `onTaskResult(String message)`
The primary Kafka message handler.
- **Topic**: `tasks.results`
- **Group ID**: `dispatcher`
- **Functionality**:
    - Deserializes the JSON message into a `TaskResult`.
    - Validates task existence and execution ID (idempotency check).
    - Transitions task states (e.g., `ASSIGNED` -> `RUNNING` -> `COMPLETED`/`FAILED`).
    - Calculates retry backoff for failed tasks.
    - Triggers resource release on the `WorkerRegistryService`.
    - Publishes completion or failure events.

## Dependencies

- **`com.cloudbalancer.common`**: Provides shared models (`TaskResult`, `TaskState`), events, and JSON utilities.
- **`com.cloudbalancer.dispatcher.persistence`**: Accesses `TaskRecord` for state persistence.
- **`com.cloudbalancer.dispatcher.service`**: Interfaces with `TaskService`, `WorkerRegistryService`, `AutoScalerService`, and `EventPublisher`.
- **`com.cloudbalancer.dispatcher.util`**: Uses `BackoffCalculator` for retry logic.
- **`org.springframework.kafka`**: Provides the `@KafkaListener` infrastructure.

## Usage Notes

### Idempotency and Stale Results
The listener implements strict idempotency checks. If a `TaskResult` contains an `executionId` that does not match the `currentExecutionId` stored in the `TaskRecord`, the result is discarded. This prevents stale results from delayed Kafka messages from overwriting newer task states.

### State Transition Logic
The listener performs "fast-forwarding" of states. If a task is received in an `ASSIGNED` state, it automatically transitions through `PROVISIONING` and `RUNNING` before reaching a terminal state. 

### Resource Management
When a task reaches a terminal state (`COMPLETED`, `FAILED`, or `TIMED_OUT`), the listener automatically invokes `workerRegistryService.releaseResources`. This is critical for preventing resource leaks in the worker cluster.

### Error Handling
The entire processing logic is wrapped in a `try-catch` block. If an exception occurs during processing, it is logged as an error, but the Kafka consumer will not automatically retry unless the exception is re-thrown (which is not currently implemented). This ensures that malformed messages do not block the partition indefinitely.

### Example Workflow
1. **Worker** finishes a task and publishes a `TaskResult` to `tasks.results`.
2. **`TaskResultListener`** consumes the message.
3. **Validation**: Checks if the `taskId` exists and if the `executionId` is current.
4. **State Update**: Transitions the `TaskRecord` to `COMPLETED` or `FAILED`.
5. **Cleanup**: Calls `workerRegistryService.releaseResources` to free up worker slots.
6. **Notification**: Publishes a `TaskCompletedEvent` or `TaskStateChangedEvent` to the `tasks.events` topic for downstream consumers.
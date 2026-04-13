# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/RetryScanner.java

## Overview

`RetryScanner` is a critical background service component within the `dispatcher` module, responsible for the automated lifecycle management of failed or timed-out tasks. It periodically polls the task repository to identify tasks that require intervention, either by re-queuing them for further execution or moving them to a dead-letter state.

**Note:** This file is a **HOTSPOT** (top 25% for both change frequency and complexity). It is a high-risk area for bugs; modifications to retry logic or poison-pill detection should be thoroughly validated via `RetryScannerTest`.

## Public API

The `RetryScanner` is managed by the Spring container and operates primarily through scheduled tasks rather than direct method invocation.

### Constructor
*   `RetryScanner(TaskRepository, TaskService, EventPublisher, long, int)`: Initializes the scanner with required persistence and messaging dependencies.
    *   `baseDelaySeconds`: Configurable via `cloudbalancer.retry.base-delay-seconds` (default: 5).
    *   `poisonPillThresholdMs`: Configurable via `cloudbalancer.retry.poison-pill-threshold-ms` (default: 2000).

### Scheduled Methods
*   `scanAndRetry()`: The primary entry point, annotated with `@Scheduled`. It executes at a fixed interval (default: 5000ms) to process tasks in `FAILED` or `TIMED_OUT` states.

## Dependencies

*   **`TaskRepository`**: Used to query tasks in terminal/error states.
*   **`TaskService`**: Used to persist state transitions (e.g., moving a task back to `QUEUED` or `DEAD_LETTERED`).
*   **`EventPublisher`**: Used to broadcast `TaskDeadLetteredEvent` to the Kafka infrastructure when a task is permanently failed.
*   **`TaskRecord`**: The domain model representing the task state and execution history.

## Usage Notes

### Retry Logic Workflow
The `scanAndRetry` method follows a strict hierarchy of checks to determine the fate of a failed task:

1.  **Backoff Check**: If `retryEligibleAt` is set and in the future, the task is skipped.
2.  **Policy Check**: If the task's `FailureAction` is set to `DEAD_LETTER`, it is immediately sent to the dead-letter queue.
3.  **Retry Limit**: The scanner calculates non-worker-caused attempts. If the count meets or exceeds `maxRetries`, the task is dead-lettered.
4.  **Poison Pill Detection**: If the task fails rapidly (under `poisonPillThresholdMs`) on three distinct workers, it is flagged as a "poison pill" and dead-lettered to prevent resource exhaustion.
5.  **Re-queue**: If all checks pass, the task is reset to `QUEUED` state, its `currentExecutionId` is regenerated, and it is returned to the task pool.

### Poison Pill Detection
The `isPoisonPill` method is a safety mechanism designed to detect tasks that crash consistently regardless of the environment. It requires:
*   At least 3 execution attempts.
*   All of the last 3 attempts must have a duration shorter than the `poisonPillThresholdMs`.
*   All of the last 3 attempts must have occurred on different `workerId`s.

### Configuration
Adjust the following properties in your Spring configuration to tune the scanner:
*   `cloudbalancer.retry.scan-interval-ms`: Frequency of the scanner loop.
*   `cloudbalancer.retry.base-delay-seconds`: Used for backoff calculations.
*   `cloudbalancer.retry.poison-pill-threshold-ms`: Defines the duration threshold for identifying "fast" failures.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/TaskService.java

## Overview

The `TaskService` is a core service component within the `dispatcher` module of the `cloudbalancer` system. It acts as the primary interface for managing the lifecycle of tasks, providing orchestration for task submission, state transitions, bulk operations (cancellation, retry, reprioritization), and persistence management.

**Note:** This file is a **HOTSPOT**. It ranks in the top 25% for both change frequency and complexity. As a central hub for task lifecycle management, modifications to this service carry a high risk of regression. Exercise extreme caution when altering state transition logic or bulk operation workflows.

## Public API

### Task Lifecycle Management
*   **`submitTask(TaskDescriptor descriptor)`**: Creates a new `TaskRecord`, initializes its state to `QUEUED`, persists it, and notifies the `AutoScalerService`.
*   **`getTask(UUID id)`**: Retrieves a `TaskEnvelope` representation of a task by its unique identifier.
*   **`getTaskRecord(UUID id)`**: Retrieves the raw `TaskRecord` entity.
*   **`updateTask(TaskRecord record)`**: Persists changes to an existing task record.

### Querying and Listing
*   **`listTasks()`**: Returns a list of all `TaskEnvelope` objects.
*   **`listTasks(int offset, int limit, TaskState status, Priority priority, ExecutorType executorType, String workerId, Instant since)`**: Returns a paginated `TaskPageResponse` based on dynamic filtering criteria.

### Bulk Operations
*   **`bulkCancel(List<UUID> taskIds)`**: Attempts to transition a list of tasks to `CANCELLED`. Returns a list of `BulkResultEntry` indicating success or failure per task.
*   **`bulkRetry(List<UUID> taskIds)`**: Resets failed or timed-out tasks to `QUEUED` status, clears the assigned worker, and generates a new execution ID.
*   **`bulkReprioritize(List<UUID> taskIds, Priority priority)`**: Updates the priority for non-terminal tasks.

### Utility
*   **`getQueuedTasks()`**: Returns all tasks in `QUEUED` state, sorted by priority (ordinal) and then by submission time.

## Dependencies

*   **`TaskRepository`**: Handles JPA-based persistence for `TaskRecord` entities.
*   **`AutoScalerService`**: Triggered upon task submission to adjust infrastructure scaling based on load.
*   **`TaskRecord` / `TaskEnvelope`**: Data models representing the internal and external state of tasks.
*   **Spring Data JPA**: Utilized for pagination and dynamic `Specification` building.

## Usage Notes

### State Transitions
The service enforces strict state transitions. For instance:
*   **Cancellation**: Only tasks in non-terminal states can be cancelled.
*   **Retry**: Only tasks in `FAILED` or `TIMED_OUT` states are eligible for retry.
*   **Reprioritization**: Only tasks in non-terminal states can have their priority modified.

### Bulk Operation Pitfalls
Bulk methods iterate through provided IDs and perform individual database operations. If a large list of IDs is provided, this may result in significant I/O overhead. Always validate the size of the input list before calling these methods.

### Example: Submitting and Retrying a Task
```java
// 1. Submit a task
TaskDescriptor descriptor = new TaskDescriptor(...);
TaskEnvelope envelope = taskService.submitTask(descriptor);

// 2. Later, if the task fails, retry it
List<UUID> idsToRetry = Collections.singletonList(envelope.getId());
List<BulkResultEntry> results = taskService.bulkRetry(idsToRetry);

if (results.get(0).isSuccess()) {
    System.out.println("Task successfully queued for retry.");
} else {
    System.err.println("Retry failed: " + results.get(0).getErrorMessage());
}
```

### Dynamic Filtering
The `listTasks` method uses a `Specification` builder (`buildSpec`). When adding new filter criteria, ensure the `Predicate` logic correctly handles `null` values to avoid runtime errors during query execution.
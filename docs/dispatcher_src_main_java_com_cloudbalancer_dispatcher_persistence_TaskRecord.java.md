# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/TaskRecord.java

## Overview

`TaskRecord` is the primary JPA entity representing the lifecycle and state of a task within the `cloudbalancer` dispatcher. It serves as the persistent database representation of a task, mapping its current status, execution history, and resource descriptors to the underlying storage layer.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Modifications to this class often have cascading effects on the task scheduling logic, persistence layer, and external API contracts. Exercise extreme caution when altering state transition logic or database mappings.

## Public API

### Lifecycle Management
*   **`static TaskRecord create(TaskDescriptor descriptor)`**: Factory method to initialize a new task. It generates a unique `UUID`, sets the initial state to `SUBMITTED`, captures the submission time, and initializes the execution history.
*   **`void transitionTo(TaskState newState)`**: Updates the task state. It enforces state machine integrity by checking `TaskState.canTransitionTo(newState)` before applying the change. Throws `IllegalStateException` if the transition is invalid.

### Data Accessors
*   **Identity & State**: `getId()`, `getState()`.
*   **Metadata**: `getPriority()`, `setPriority()`, `getExecutorType()`, `getSubmittedAt()`, `getDescriptor()`.
*   **Execution Tracking**: `getAssignedWorkerId()`, `setAssignedWorkerId()`, `getAssignedAt()`, `setAssignedAt()`, `getStartedAt()`, `setStartedAt()`, `getCompletedAt()`, `setCompletedAt()`, `getRetryEligibleAt()`, `setRetryEligibleAt()`, `getCurrentExecutionId()`, `setCurrentExecutionId()`.
*   **Diagnostics**: `getLastStdout()`, `setLastStdout()`, `getLastStderr()`, `setLastStderr()`, `getExecutionHistory()`, `addAttempt(ExecutionAttempt attempt)`.

### Serialization
*   **`TaskEnvelope toEnvelope()`**: Converts the internal entity into a `TaskEnvelope` DTO for external API consumption.

## Dependencies

*   **`jakarta.persistence`**: Used for ORM mapping (Entity, Table, Column, etc.).
*   **`com.cloudbalancer.common.model`**: Provides core domain objects (`TaskDescriptor`, `TaskState`, `Priority`, `ExecutorType`, `ExecutionAttempt`, `TaskEnvelope`).
*   **Converters**: Relies on `TaskDescriptorConverter` and `ExecutionHistoryConverter` for JSONB mapping in the database.

## Usage Notes

### State Transitions
The `transitionTo` method is the only safe way to modify the task state. Direct manipulation of the `state` field is discouraged as it bypasses the validation logic defined in the `TaskState` enum. Always ensure the state machine definition in `TaskState` is updated if new lifecycle stages are introduced.

### Handling JSONB Fields
The `descriptor` and `executionHistory` fields are mapped as `jsonb` in the database. When updating these fields, ensure the associated converters are correctly configured to handle the serialization of the `TaskDescriptor` and `List<ExecutionAttempt>` objects.

### Example: Creating and Updating a Task
```java
// 1. Create a new task record
TaskDescriptor descriptor = new TaskDescriptor(...);
TaskRecord task = TaskRecord.create(descriptor);

// 2. Persist to database (via Repository)
taskRepository.save(task);

// 3. Transition state
task.transitionTo(TaskState.ASSIGNED);
task.setAssignedWorkerId("worker-01");
task.setAssignedAt(Instant.now());

// 4. Update stdout/stderr during execution
task.setLastStdout("Task started successfully...");
```

### Common Pitfalls
*   **Concurrency**: As a JPA entity, `TaskRecord` instances are not inherently thread-safe. Ensure that updates are performed within the context of a transaction, preferably using optimistic locking if the system scales to high-frequency updates.
*   **Hotspot Risk**: Because this is a high-activity file, ensure that any changes to the schema (e.g., adding columns) are accompanied by appropriate database migrations and that the `toEnvelope` method is updated to expose new fields to the UI/API.
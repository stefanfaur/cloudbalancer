# File: common/src/main/java/com/cloudbalancer/common/model/TaskEnvelope.java

## Overview

`TaskEnvelope` is a core domain model in the `cloudbalancer` system that acts as a container for a task's lifecycle metadata. It wraps a `TaskDescriptor` with state tracking, submission timestamps, and a comprehensive history of execution attempts. This class is designed to manage the state machine of a task, ensuring that transitions between states are valid and that the execution history remains auditable.

## Public API

### Constructors & Factories
*   **`create(TaskDescriptor descriptor)`**: A static factory method that initializes a new `TaskEnvelope` with a unique `UUID`, the current system time as the submission timestamp, and an initial state of `TaskState.SUBMITTED`.
*   **`fromJson(...)`**: A `@JsonCreator` annotated factory method used for deserialization from JSON, allowing the reconstruction of an existing `TaskEnvelope` with its full history.

### State Management
*   **`transitionTo(TaskState newState)`**: Updates the task's state. It validates the transition against the current state; if the transition is invalid, it throws an `IllegalStateTransitionException`.
*   **`addAttempt(ExecutionAttempt attempt)`**: Appends a new `ExecutionAttempt` to the internal history list.

### Getters
*   **`getId()`**: Returns the unique `UUID` of the task.
*   **`getDescriptor()`**: Returns the `TaskDescriptor` associated with this envelope.
*   **`getSubmittedAt()`**: Returns the `Instant` when the task was created.
*   **`getState()`**: Returns the current `TaskState`.
*   **`getExecutionHistory()`**: Returns an unmodifiable view of the list of `ExecutionAttempt` objects.

## Dependencies

*   **Jackson Annotations**: Used for JSON serialization/deserialization (`@JsonCreator`, `@JsonProperty`).
*   **Java Time API**: Uses `java.time.Instant` for precise timestamping.
*   **Internal Models**: Relies on `TaskDescriptor`, `TaskState`, and `ExecutionAttempt` for domain-specific logic.

## Usage Notes

*   **Immutability**: While the `TaskEnvelope` object itself is mutable (to allow for state transitions and history updates), the `executionHistory` list is exposed via an unmodifiable wrapper to prevent external modification of the history log.
*   **State Validation**: Always use `transitionTo()` to change the task state. Direct modification of the state field is not supported, ensuring that the system's state machine integrity is maintained.
*   **Serialization**: This class is intended to be used with Jackson. When deserializing, ensure the JSON structure matches the fields expected by the `fromJson` factory method.
*   **Error Handling**: Be prepared to catch `IllegalStateTransitionException` when invoking `transitionTo()` if the business logic permits potentially invalid state changes.
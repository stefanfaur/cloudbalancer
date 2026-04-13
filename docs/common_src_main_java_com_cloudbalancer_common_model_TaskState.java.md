# File: common/src/main/java/com/cloudbalancer/common/model/TaskState.java

## Overview

The `TaskState` enum defines the canonical lifecycle states for tasks managed within the CloudBalancer system. It provides a robust state machine implementation that enforces valid transitions between states and identifies terminal states where task processing concludes.

This enum serves as the central authority for task progression, ensuring consistency across the backend services and the frontend dashboard.

## Public API

### `TaskState` (Enum)
The following states are defined:
* `SUBMITTED`: Initial state upon task creation.
* `VALIDATED`: Task parameters have been verified.
* `QUEUED`: Task is waiting in the scheduler queue.
* `ASSIGNED`: Task has been assigned to a worker node.
* `PROVISIONING`: Resources are being prepared for execution.
* `RUNNING`: Task is actively executing.
* `POST_PROCESSING`: Task has finished execution and is performing cleanup or result aggregation.
* `COMPLETED`: Task finished successfully.
* `FAILED`: Task execution failed.
* `TIMED_OUT`: Task exceeded its allocated execution time.
* `CANCELLED`: Task was manually aborted.
* `DEAD_LETTERED`: Task failed repeatedly and has been moved to a dead-letter queue for manual intervention.

### Methods

#### `canTransitionTo(TaskState target)`
Checks if the current state is permitted to transition to the specified `target` state based on the predefined transition matrix.
* **Returns**: `boolean` - `true` if the transition is allowed, `false` otherwise.

#### `isTerminal()`
Determines if the current state represents the end of the task lifecycle.
* **Returns**: `boolean` - `true` if the state is `COMPLETED`, `CANCELLED`, `TIMED_OUT`, `FAILED`, or `DEAD_LETTERED`.

## Dependencies

* `java.util.Map`: Used to store the transition matrix.
* `java.util.Set`: Used to define the collection of allowed next states for each enum constant.

## Usage Notes

* **State Machine Integrity**: The `VALID_TRANSITIONS` map acts as a strict guardrail. Any attempt to transition to an undefined state will return `false` via `canTransitionTo`.
* **Retry Logic**: Note that `FAILED` and `TIMED_OUT` states allow a transition back to `QUEUED`, facilitating automatic retry mechanisms.
* **Terminal States**: Once a task enters a terminal state (`isTerminal() == true`), it cannot transition to any other state. Ensure that logic relying on task status accounts for these final states to prevent unexpected behavior in downstream services.
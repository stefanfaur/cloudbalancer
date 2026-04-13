# File: common/src/main/java/com/cloudbalancer/common/model/IllegalStateTransitionException.java

## Overview

The `IllegalStateTransitionException` is a custom runtime exception used within the CloudBalancer system to signal that an invalid state change has been attempted on a task. It is designed to be thrown when the state machine logic encounters a transition from a `TaskState` that is not permitted by the system's business rules.

## Public API

### Class: `IllegalStateTransitionException`

Extends `java.lang.RuntimeException`.

#### Constructors
*   `IllegalStateTransitionException(TaskState from, TaskState to)`
    *   Creates a new exception instance with a descriptive error message detailing the invalid transition from the source state to the target state.

#### Methods
*   `TaskState getFrom()`
    *   Returns the `TaskState` from which the transition was attempted.
*   `TaskState getTo()`
    *   Returns the `TaskState` to which the transition was attempted.

## Dependencies

*   `com.cloudbalancer.common.model.TaskState`: Used to define the source and target states involved in the invalid transition.

## Usage Notes

*   This exception is intended for use within the task management logic where `TaskState` transitions are validated.
*   Because it is a `RuntimeException`, it does not require explicit `try-catch` blocks in the calling code, though it should be handled at the service or controller layer to provide meaningful feedback to the user or API consumer.
*   When catching this exception, the `getFrom()` and `getTo()` methods can be used to programmatically log or report the specific invalid transition path for debugging purposes.
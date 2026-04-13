# File: common/src/main/java/com/cloudbalancer/common/model/FailureAction.java

## Overview

The `FailureAction` enum defines the set of strategies available to the CloudBalancer system when a task or operation encounters a failure. It serves as a configuration model to dictate how the system should handle errors during execution, ensuring consistent error recovery workflows across the platform.

## Public API

The `FailureAction` enum provides the following constants:

*   **`RETRY`**: Indicates that the system should attempt to re-execute the failed operation, typically following a backoff strategy.
*   **`DEAD_LETTER`**: Indicates that the failed operation should be moved to a dead-letter queue or storage for manual inspection or later processing.
*   **`CALLBACK`**: Indicates that the system should trigger a predefined callback mechanism to notify an external service or user of the failure.

## Dependencies

This enum is a standalone component within the `com.cloudbalancer.common.model` package and does not depend on any external libraries or other internal classes.

## Usage Notes

- **Extensibility**: This enum is intended to be used in conjunction with task processing logic to determine the flow of control after an exception occurs.
- **Integration**: When implementing new task types or state machines within the CloudBalancer system, use `FailureAction` to standardize how failures are handled, ensuring compatibility with the broader system architecture.
- **Context**: This enum is frequently referenced in logic involving state transitions, such as those handled by `IllegalStateTransitionException`, to define the post-failure state of a task.
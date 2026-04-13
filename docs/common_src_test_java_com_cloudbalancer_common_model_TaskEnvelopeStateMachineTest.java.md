# File: common/src/test/java/com/cloudbalancer/common/model/TaskEnvelopeStateMachineTest.java

## Overview

`TaskEnvelopeStateMachineTest` is a critical JUnit 5 test suite responsible for validating the state machine logic of the `TaskEnvelope` domain model. It ensures that task lifecycle transitions adhere to strict business rules, preventing invalid state changes and verifying that terminal states are correctly identified.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it governs the core state transitions of the task execution engine, any modifications to this test suite or the underlying `TaskEnvelope` state logic carry a high risk of introducing system-wide regressions.

## Public API

The test class does not expose a public API for production use, as it is a test-only utility. However, it exercises the following key methods of the `TaskEnvelope` model:

*   **`transitionTo(TaskState)`**: The primary method for moving a task between states. The tests verify that this method correctly throws `IllegalStateTransitionException` when an invalid transition is attempted.
*   **`addAttempt(ExecutionAttempt)`**: Used to track the history of task execution attempts.
*   **`getState()`**: Retrieves the current `TaskState` of the envelope.
*   **Serialization/Deserialization**: Uses Jackson to ensure the `TaskEnvelope` can be persisted and recovered without losing state or metadata.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management (`@Test`, `@ParameterizedTest`).
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **Jackson**: Used for JSON serialization/deserialization testing.
*   **`TaskEnvelope`**: The target class under test.
*   **`TaskState`**: The enum defining valid states and terminal status.

## Usage Notes

### Testing Lifecycle Transitions
The test suite is organized into three logical categories:
1.  **Valid Transitions**: Tests like `fullHappyPath` and `failedToQueuedForRetry` verify that the state machine allows expected business flows, including retry logic.
2.  **Invalid Transitions**: Tests like `submittedCannotGoDirectlyToRunning` and `completedCannotTransitionAnywhere` ensure that the state machine enforces strict boundaries, preventing illegal jumps or transitions from terminal states.
3.  **Terminal State Detection**: Uses `@ParameterizedTest` to verify that `isTerminal()` returns the correct boolean for all `TaskState` values.

### Adding New Transitions
If you are modifying the `TaskEnvelope` state machine:
1.  **Update the State Machine**: Modify the `transitionTo` logic in `TaskEnvelope.java`.
2.  **Add a Test Case**: Create a new test method in this file following the pattern of existing tests.
3.  **Verify Serialization**: Always ensure that new fields added to `TaskEnvelope` are included in the `envelopeSerializationRoundTrip` test to prevent data loss during persistence.

### Example: Adding a New State
If a new state (e.g., `SUSPENDED`) is added, you must:
1.  Update `TaskState` enum.
2.  Update `nonTerminalStatesAreNotTerminal` in this test file to include the new state.
3.  Add a test case verifying the valid entry and exit transitions for `SUSPENDED`.

### Pitfalls
*   **Terminal State Violations**: Ensure that any state marked as terminal in `TaskState` cannot transition to any other state. The `completedCannotTransitionAnywhere` test is a template for verifying this.
*   **History Tracking**: When adding new state-related metadata, ensure `addAttemptTracksHistory` is updated to verify that the history remains consistent across state changes.
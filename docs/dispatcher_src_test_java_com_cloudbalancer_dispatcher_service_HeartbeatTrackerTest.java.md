# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/HeartbeatTrackerTest.java

## Overview

`HeartbeatTrackerTest` is a comprehensive unit test suite for the `HeartbeatTracker` component within the `dispatcher` module. It validates the logic responsible for monitoring worker health, managing state transitions (e.g., `HEALTHY` to `SUSPECT` to `DEAD`), and handling lifecycle events such as `DRAINING` or `STOPPING`.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for bugs, as it governs the critical failure detection logic for the entire worker cluster. Changes to this test suite should be approached with extreme caution, as they directly impact the reliability of the dispatcher's worker management.

## Public API

The test suite exercises the following primary methods of `HeartbeatTracker`:

*   **`onHeartbeat(String workerId, Instant timestamp)`**: Validates that incoming heartbeats correctly update the last-seen registry and trigger state recovery for `SUSPECT` workers.
*   **`checkLiveness()`**: Validates the periodic background task that evaluates worker health against configured thresholds (`SUSPECT_THRESHOLD` and `DEAD_THRESHOLD`).

## Dependencies

*   **`WorkerRepository`**: Mocked to simulate persistence layer interactions.
*   **`WorkerFailureHandler`**: Mocked to verify that failure callbacks (e.g., `onWorkerDead`) are triggered correctly when a worker is declared dead.
*   **`Mockito`**: Used for dependency injection and behavior verification.
*   **`AssertJ`**: Used for fluent assertions on worker state transitions.

## Usage Notes

### State Transition Logic
The `HeartbeatTracker` enforces specific state machine rules validated by this test suite:

1.  **Suspect Transition**: If a worker has not sent a heartbeat within 30 seconds, it transitions to `SUSPECT`.
2.  **Dead Transition**: If a worker has not sent a heartbeat within 60 seconds, it transitions to `DEAD`, triggering the `WorkerFailureHandler`.
3.  **Recovery**: A `SUSPECT` worker that sends a heartbeat immediately reverts to `HEALTHY`.
4.  **Lifecycle Protection**:
    *   `DRAINING` workers do not transition back to `HEALTHY` on heartbeat; they remain `DRAINING` until they either finish or time out.
    *   `STOPPING` workers have their own timeout logic based on `stoppingStartedAt`. They are immune to standard heartbeat-based recovery.

### Testing Edge Cases
*   **Grace Period**: New workers are protected from immediate `checkLiveness` failures by using their `registeredAt` timestamp as a fallback if no heartbeat has been received.
*   **Idempotency**: `alreadyDeadWorkerStaysDeadOnLivenessCheck` ensures that the system does not repeatedly trigger failure handlers for workers already marked as `DEAD`.
*   **Draining/Stopping**: These tests ensure that workers in terminal or transitionary states are not accidentally "promoted" back to `HEALTHY` by stray heartbeats.

### Example: Verifying a Failure Transition
To add a new test case for a custom health state, follow this pattern:

```java
@Test
void testCustomStateTransition() {
    // 1. Setup: Create worker in specific state
    var worker = createWorker("w-test", WorkerHealthState.SOME_STATE);
    
    // 2. Arrange: Mock repository to return the worker
    when(workerRepository.findAll()).thenReturn(List.of(worker));
    
    // 3. Act: Trigger the liveness check
    tracker.checkLiveness();
    
    // 4. Assert: Verify state change and repository interaction
    assertThat(worker.getHealthState()).isEqualTo(WorkerHealthState.DEAD);
    verify(workerRepository).save(worker);
}
```

**Warning:** When modifying the `SUSPECT_THRESHOLD` or `DEAD_THRESHOLD` constants in the test, ensure they remain synchronized with the production `application.yml` configuration to prevent drift between test assumptions and runtime behavior.
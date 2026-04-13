# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scaling/PendingWorkerTrackerTest.java

## Overview

`PendingWorkerTrackerTest` is a JUnit 5 test suite that validates the functionality of the `PendingWorkerTracker` class. This component is responsible for tracking the lifecycle of worker provisioning requests, ensuring that pending operations are correctly registered, resolved, failed, or expired based on their state and age.

## Public API

The test class validates the following behaviors of the `PendingWorkerTracker`:

*   **`markPendingIncreasesCount`**: Verifies that registering a new worker request correctly increments the internal pending count.
*   **`resolveDecreasesCount`**: Ensures that successfully resolving a pending worker request removes it from the tracker and decrements the count.
*   **`failDecreasesCount`**: Confirms that marking a worker request as failed removes it from the tracker and decrements the count.
*   **`expireStaleRemovesOldEntries`**: Validates that the tracker correctly identifies and removes worker entries that exceed a specified age threshold.
*   **`resolveUnknownWorkerIsNoOp`**: Checks that attempting to resolve a worker ID not currently tracked does not cause errors or affect the count.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management (`@BeforeEach`) and assertions (`@Test`).
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **Java Time API**: Used for handling `Instant` and `Duration` to simulate temporal states in the tracker.
*   **`PendingWorkerTracker`**: The system under test (SUT) located in `com.cloudbalancer.dispatcher.scaling`.

## Usage Notes

*   **Test Isolation**: Each test case initializes a fresh instance of `PendingWorkerTracker` via the `@BeforeEach` `setUp` method to ensure state independence.
*   **Temporal Testing**: The `expireStaleRemovesOldEntries` test utilizes `Instant.now().minus(Duration.ofSeconds(120))` to simulate stale data, demonstrating how the tracker handles time-based cleanup logic.
*   **Robustness**: The `resolveUnknownWorkerIsNoOp` test ensures that the tracker gracefully handles unexpected or non-existent worker IDs without throwing exceptions.
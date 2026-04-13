# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/HeartbeatIntegrationTest.java

## Overview

`HeartbeatIntegrationTest` is a critical integration test suite for the `dispatcher` module. It validates the end-to-end lifecycle of worker health monitoring, specifically ensuring that `HeartbeatListener` correctly consumes Kafka events and that the `HeartbeatTracker` accurately updates the `WorkerRepository` state based on these events.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it manages state transitions across distributed components (Kafka, Database, and internal schedulers), it is a high-risk area for regressions.

## Public API

The class provides the following test methods to verify system behavior:

*   **`heartbeatViaKafkaSetsWorkerHealthy`**: Simulates a worker registration followed by a heartbeat event. Verifies that the system correctly processes the heartbeat and maintains the `HEALTHY` status in the database.
*   **`livenessTransitionsPersistedToSuspect`**: Simulates a worker registration and an initial heartbeat, then ceases further heartbeats. Verifies that the system's internal liveness check logic correctly transitions the worker state to `SUSPECT` or `DEAD` based on the configured thresholds.

## Dependencies

*   **Infrastructure**: `TestContainersConfig` (Kafka container) for isolated integration testing.
*   **Persistence**: `WorkerRepository` for verifying state changes in the database.
*   **Messaging**: `KafkaProducer` for simulating external worker events.
*   **Testing Utilities**: `Awaitility` for handling asynchronous assertions and `JUnit 5` for test lifecycle management.

## Usage Notes

### Configuration
The test suite overrides default heartbeat thresholds to ensure fast execution:
*   `cloudbalancer.dispatcher.heartbeat-suspect-threshold-seconds=2`
*   `cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds=4`
*   `cloudbalancer.dispatcher.liveness-check-interval-ms=500`

### Implementation Rationale
*   **Asynchronous Verification**: Because health transitions occur in background threads (via `HeartbeatTracker`), the tests use `Awaitility` to poll the database until the expected state is reached, rather than relying on brittle `Thread.sleep()` calls.
*   **Isolation**: The `tearDown()` method ensures the `WorkerRepository` is cleared after every test, preventing state leakage between test cases.

### Potential Pitfalls
*   **Timing Sensitivity**: If the CI/CD environment is under heavy load, the `await()` timeouts (currently 10 seconds) might be reached. If tests fail intermittently, consider increasing the timeout or the `pollInterval`.
*   **Kafka Connectivity**: As this test relies on `TestContainers`, ensure the environment supports Docker-in-Docker or has the necessary permissions to spawn containers.
*   **State Race Conditions**: The tests rely on the order of events in Kafka. Ensure that the registration event is fully processed (verified by `workerRepository.findById`) before sending the heartbeat event to avoid race conditions where the heartbeat is ignored because the worker is not yet registered.

### Example Workflow
To add a new test case for a different health state (e.g., `UNHEALTHY`):
1.  Register a worker via `workers.registration` topic.
2.  Wait for the worker to appear in `WorkerRepository`.
3.  Send a `WorkerHeartbeatEvent` with `WorkerHealthState.UNHEALTHY` via the `workers.heartbeat` topic.
4.  Use `await().until(...)` to verify that `workerRepository.findById(workerId).get().getHealthState()` returns `UNHEALTHY`.
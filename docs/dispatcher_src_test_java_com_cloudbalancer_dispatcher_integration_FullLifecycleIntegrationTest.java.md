# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/FullLifecycleIntegrationTest.java

## Overview

`FullLifecycleIntegrationTest` is a critical integration test suite for the `dispatcher` module. It validates the end-to-end task execution lifecycle by simulating the interaction between the dispatcher service, the message broker (Kafka), and external worker nodes.

**Note:** This file is a **HOTSPOT** (top 25% for change frequency and complexity). It serves as the primary verification point for task state transitions and scheduling logic. Changes to the task submission API or the worker communication protocol require careful validation against this suite to prevent regressions.

## Public API

The class provides the following test methods to verify system behavior:

*   **`submitTaskFlowsToCompletedViaWorker()`**: Verifies that a standard task submitted via the REST API transitions from `QUEUED` to `COMPLETED` when processed by a simulated worker.
*   **`taskWithHighFailProbabilityReachesFailed()`**: Validates error handling by submitting a task configured to fail (100% probability) and asserting that the system correctly marks the task as `FAILED`.
*   **`multipleConcurrentTasksAllComplete()`**: Tests the dispatcher's ability to handle high-volume, concurrent task submissions and ensures all tasks reach a terminal state (`COMPLETED` or `FAILED`) within the expected timeframe.

## Dependencies

*   **Spring Boot Test**: Provides the `@SpringBootTest` context and `RestClient` for API interaction.
*   **Testcontainers (Kafka)**: Manages an ephemeral Kafka instance required for task distribution.
*   **Awaitility**: Used for asynchronous polling, essential for verifying state changes that occur in background threads.
*   **CloudBalancer Common Models**: Relies on `TaskDescriptor`, `TaskEnvelope`, `TaskState`, and `ResourceProfile` to define and inspect task lifecycles.
*   **TestWorkerSimulator**: A helper component used to mimic worker behavior, consuming tasks from Kafka and reporting results back to the dispatcher.

## Usage Notes

### Test Environment Setup
The test suite requires an active Kafka broker. It uses `TestContainersConfig` to provision the infrastructure. 
1.  **Authentication**: The `setUp` method performs an automated login against the `/api/auth/login` endpoint to obtain a JWT, which is then injected into the `RestClient` headers for all subsequent requests.
2.  **Worker Simulation**: A `TestWorkerSimulator` is instantiated and started in `setUp`. It is crucial that the worker is closed in `tearDown` to prevent resource leaks and port conflicts in the test environment.

### Asynchronous Assertions
Because task processing is asynchronous, tests utilize `Awaitility`. 
*   **Polling**: Tests poll the `/api/tasks/{id}` endpoint with a defined `pollInterval`.
*   **Timeouts**: Most tests are configured with a 30-60 second timeout. If your environment is resource-constrained (e.g., CI/CD pipelines), ensure these timeouts are sufficient to avoid flaky tests.

### Common Pitfalls
*   **Propagation Delay**: The `Thread.sleep(2000)` in `setUp` is a heuristic to allow worker registration to propagate. If tests fail intermittently during initialization, this delay may need to be increased or replaced with a more robust readiness check.
*   **State Consistency**: When testing concurrent tasks, always verify that the system reaches a terminal state. The `multipleConcurrentTasksAllComplete` test specifically checks that all tasks eventually reach `COMPLETED` or `FAILED` before asserting the final success condition.
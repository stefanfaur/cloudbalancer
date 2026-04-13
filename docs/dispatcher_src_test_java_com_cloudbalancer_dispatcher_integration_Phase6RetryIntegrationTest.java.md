# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/Phase6RetryIntegrationTest.java

## Overview

`Phase6RetryIntegrationTest` is a critical integration test suite within the `dispatcher` module. It is designed to validate the system's resilience by testing the automated retry and dead-lettering (DLQ) lifecycle of tasks. 

**Note**: This file is a **HOTSPOT** (top 25% for change frequency and complexity). It represents a high-risk area for regressions in task scheduling and fault-tolerance logic.

The test suite simulates a task execution environment where a task is configured to fail consistently (`failProbability=1.0`). It verifies that the dispatcher correctly tracks the retry count, honors the `ExecutionPolicy`, and transitions the task to the `DEAD_LETTERED` state once the maximum retry threshold is reached.

## Public API

The class provides the following test-lifecycle and validation methods:

*   **`setUp()`**: Initializes the `RestClient` with administrative credentials and starts a `TestWorkerSimulator` linked to the test Kafka container.
*   **`tearDown()`**: Ensures the `TestWorkerSimulator` is properly closed after each test execution to prevent resource leaks.
*   **`taskFailsAndIsEventuallyDeadLettered()`**: The primary test case. It submits a task with a `maxRetries` value of 2 and asserts that the system eventually marks the task as `DEAD_LETTERED` after the retries are exhausted, while also validating that the `executionHistory` contains the expected number of failed attempts.

## Dependencies

This test relies on the following key components:

*   **Spring Boot Test**: Uses `@SpringBootTest` with `RANDOM_PORT` to spin up the dispatcher context.
*   **Testcontainers**: Utilizes `KafkaContainer` (via `TestContainersConfig`) to simulate the message broker environment required for task orchestration.
*   **Awaitility**: Used for asynchronous polling, allowing the test to wait for the eventually consistent state of the task in the database/API.
*   **TestWorkerSimulator**: A helper utility that simulates a worker node to consume and fail tasks.
*   **RestClient**: Used to interact with the dispatcher's REST API for task submission and status verification.

## Usage Notes

### Implementation Rationale
The test uses a `fixed` backoff strategy and specific properties (`cloudbalancer.retry.scan-interval-ms=1000`) to ensure the test executes in a deterministic timeframe. By setting `failProbability` to 1.0, the test forces the system into the worst-case scenario for task execution, ensuring that the retry loop is fully exercised.

### Potential Pitfalls
*   **Timing Sensitivity**: Because this is an integration test involving asynchronous message processing (Kafka), the `await()` timeout is set to 60 seconds. If the CI environment is under heavy load, this duration might need adjustment.
*   **Authentication**: The `setUp` method performs a programmatic login to obtain a JWT. If the authentication schema or the default "admin" credentials change, this test will fail during the initialization phase.
*   **Resource Cleanup**: Failure to properly close the `TestWorkerSimulator` in `tearDown` can lead to orphaned Kafka consumers, which may interfere with subsequent tests in the suite.

### Example: Validating Task Lifecycle
To verify that a task is correctly handled during a failure, the test performs the following steps:
1.  **Submission**: POST a `TaskDescriptor` with `FailureAction.RETRY` and `maxRetries=2`.
2.  **Polling**: Use `await().untilAsserted(...)` to repeatedly query the task status via `GET /api/tasks/{id}`.
3.  **Assertion**: Verify the state transitions from `QUEUED` to `DEAD_LETTERED`.
4.  **History Check**: Validate that `getExecutionHistory()` contains at least 2 entries, confirming that the dispatcher attempted the task multiple times before giving up.
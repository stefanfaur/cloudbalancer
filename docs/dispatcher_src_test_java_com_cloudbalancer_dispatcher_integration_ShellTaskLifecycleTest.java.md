# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/ShellTaskLifecycleTest.java

## Overview

`ShellTaskLifecycleTest` is a critical integration test suite for the CloudBalancer dispatcher module. It validates the end-to-end lifecycle of tasks, ensuring that the dispatcher correctly routes tasks to capable workers based on `ExecutorType`. 

**Note:** This file is a **HOTSPOT** within the repository, ranking in the top 25% for both change frequency and complexity. It serves as a primary verification point for task scheduling logic and is a high-risk area for regressions in the dispatcher's core functionality.

## Public API

The class provides the following test methods to verify system behavior:

*   **`setUp()`**: Initializes the integration environment, including cleaning the database, authenticating as an admin, and starting a `TestWorkerSimulator` configured with `SHELL` and `SIMULATED` executor capabilities.
*   **`tearDown()`**: Ensures the `TestWorkerSimulator` is properly shut down after each test execution to prevent resource leaks.
*   **`shellTaskCompletesSuccessfully()`**: Submits a `SHELL` type task and verifies that the dispatcher successfully assigns it to the worker and transitions the task state to `COMPLETED`.
*   **`dockerTaskStaysQueuedWhenNoDockerCapableWorkerExists()`**: Submits a `DOCKER` type task and verifies that the dispatcher correctly leaves the task in a `QUEUED` state when no registered worker advertises support for the `DOCKER` executor.

## Dependencies

This test relies on the following infrastructure and internal components:

*   **Spring Boot Test**: Uses `@SpringBootTest` with `RANDOM_PORT` for full context loading.
*   **TestContainers**: Utilizes `KafkaContainer` for message-driven communication between the dispatcher and workers.
*   **Persistence Layer**: Accesses `TaskRepository` and `WorkerRepository` to manage state during test setup and teardown.
*   **REST Client**: Uses `RestClient` to interact with the dispatcher's API endpoints.
*   **TestWorkerSimulator**: A helper component that mimics worker behavior, allowing for controlled task execution scenarios.

## Usage Notes

### Implementation Rationale
The test suite uses `Awaitility` to handle the asynchronous nature of the task lifecycle. Because task assignment and execution occur across different threads and processes (via Kafka), polling is necessary to verify state transitions.

### Potential Pitfalls
*   **Stale State**: The `setUp()` method explicitly deletes all records from `taskRepository` and `workerRepository`. If tests are run in parallel or if the database schema changes, this may lead to intermittent failures.
*   **Timing Sensitivity**: The `Thread.sleep(2000)` in `setUp()` is used to allow worker registration to propagate to the dispatcher. If the environment is under heavy load, this duration may need adjustment.
*   **Hotspot Risk**: As a high-activity file, changes to the `TaskDescriptor` model or the `TaskState` machine will likely require updates here. Always run this test suite when modifying the dispatcher's scheduling or worker-matching logic.

### Example: Adding a New Executor Type Test
To test a new executor type (e.g., `KUBERNETES`), follow this pattern:
1.  Update the `TestWorkerSimulator` in `setUp` to include the new `ExecutorType` in the supported set.
2.  Create a new test method that submits a `TaskDescriptor` with the new `ExecutorType`.
3.  Use `await()` to verify the task reaches the expected terminal state (`COMPLETED` or `FAILED`).
4.  Ensure the `tearDown` method handles the new worker configuration if necessary.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/Phase6WorkerDeathIntegrationTest.java

## Overview

`Phase6WorkerDeathIntegrationTest` is a critical integration test suite designed to validate the fault-tolerance and self-healing capabilities of the `dispatcher` module. It specifically tests the system's ability to detect worker failures, re-queue orphaned tasks, and ensure that surviving workers successfully complete the re-queued workload.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a core component of the system's reliability testing, changes to this file or the underlying recovery logic should be treated as high-risk.

## Public API

The class provides the following test lifecycle and execution methods:

*   **`setUp()`**: Initializes the test environment, including authentication via the `RestClient` and the instantiation of two `TestWorkerSimulator` instances. It ensures the system is in a ready state before tests execute.
*   **`tearDown()`**: Ensures clean resource disposal by closing both worker simulators after test execution.
*   **`workerDeathRequeuesTasksToSurvivor()`**: The primary test case. It submits tasks, triggers a simulated worker failure via `ChaosMonkeyService`, and uses `Awaitility` to verify that tasks are recovered and completed by the surviving node.

## Dependencies

This test relies on the following infrastructure and service components:

*   **Spring Boot Test**: Uses `@SpringBootTest` to spin up the application context with specific configuration properties (e.g., `heartbeat-dead-threshold-seconds`).
*   **TestContainers**: Uses `KafkaContainer` to provide a real message broker for task distribution.
*   **ChaosMonkeyService**: An internal service used to inject faults (killing specific workers) during the test execution.
*   **TestWorkerSimulator**: A helper class that mimics worker behavior, allowing the dispatcher to track and assign tasks to simulated nodes.
*   **Awaitility**: Essential for handling asynchronous verification of task states in a distributed environment.

## Usage Notes

### Test Logic Flow
1.  **Initialization**: Two workers are registered with the dispatcher.
2.  **Task Submission**: Multiple `TaskDescriptor` objects are submitted to the API.
3.  **Failure Injection**: The `ChaosMonkeyService` is invoked to terminate one of the workers.
4.  **Verification**: The test polls the API to ensure that tasks previously assigned to the dead worker transition to a terminal state (`COMPLETED`) via the surviving worker.

### Configuration Pitfalls
The test overrides several critical configuration properties:
*   `cloudbalancer.dispatcher.heartbeat-dead-threshold-seconds=120`: This defines how long the dispatcher waits before declaring a worker dead. If this value is changed, the `Awaitility` timeouts in the test must be adjusted accordingly, or the test will flake.
*   **Timing**: Because this is an integration test involving Kafka and network calls, ensure the environment has sufficient resources to handle the overhead of multiple containers.

### Troubleshooting
*   **Flaky Tests**: If `workerDeathRequeuesTasksToSurvivor` fails, check the `heartbeat-dead-threshold-seconds`. If the dispatcher takes longer than the test's `await()` timeout to detect the death, the test will fail.
*   **Authentication**: The `setUp` method performs a login to obtain a JWT. If the authentication service is unreachable or the default credentials (`admin/admin`) are changed, the test will fail during initialization.
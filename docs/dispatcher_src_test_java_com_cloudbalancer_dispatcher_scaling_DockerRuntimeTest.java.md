# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scaling/DockerRuntimeTest.java

## Overview

`DockerRuntimeTest` is a comprehensive JUnit 5 test suite responsible for validating the lifecycle management of Docker-based workers within the `cloudbalancer` system. It ensures that the `DockerRuntime` class correctly interacts with the Docker daemon to provision, monitor, and terminate worker containers.

**Note**: This file is a **HOTSPOT** within the repository, characterized by high change frequency and significant complexity. It is a high-risk area for bugs, as it governs the infrastructure-level orchestration of worker nodes.

## Public API

The test suite validates the following primary operations of the `DockerRuntime` class:

*   **`startWorker(WorkerConfig config)`**: Verifies that the runtime correctly translates a `WorkerConfig` into a Docker container creation request, applies environment variables, configures networking, and initiates the container.
*   **`stopWorker(String workerId)`**: Ensures that the runtime correctly identifies the container associated with a worker ID, stops the container with a defined timeout, and removes it from the Docker host.
*   **`drainAndStop(String workerId, int delaySeconds)`**: Validates the graceful shutdown sequence, confirming that a `DrainCommand` is published to the event bus before the container is scheduled for termination.

## Dependencies

The test suite relies on the following external and internal components:

*   **JUnit 5 & Mockito**: Used for test execution and mocking the Docker daemon interactions.
*   **Docker Java API**: The underlying library used by `DockerRuntime` to communicate with the Docker engine.
*   **`ScalingProperties`**: Provides configuration parameters such as worker image names, network settings, and drain timeouts.
*   **`WorkerRegistryService`**: Mocked to verify that worker state transitions are correctly recorded.
*   **`EventPublisher`**: Mocked to verify that lifecycle events (registration and drain commands) are correctly propagated through the system.

## Usage Notes

### Testing Lifecycle Management
The tests utilize `MockitoExtension` to inject mocks for all Docker command interfaces (`CreateContainerCmd`, `StartContainerCmd`, etc.). This allows for testing complex container orchestration logic without requiring a running Docker daemon.

### Handling Asynchronous Operations
The `drainAndStopPublishesDrainCommandAndSchedulesStop` test demonstrates how the suite handles asynchronous behavior. It uses `Thread.sleep()` to simulate the passage of time required for the scheduled stop task to execute after the drain command is published. 

### Common Pitfalls
1.  **State Leakage**: Because `DockerRuntime` maintains an internal map of tracked containers, ensure that tests are isolated. The `setUp` method initializes a fresh `DockerRuntime` instance for every test case to prevent state leakage.
2.  **Docker API Exceptions**: The `startWorkerReturnsFalseOnDockerError` test specifically covers scenarios where the Docker daemon is unreachable or returns an error. Developers modifying `DockerRuntime` must ensure that these exceptions are caught and handled gracefully to prevent the dispatcher from crashing.
3.  **Mocking Depth**: When adding new functionality, ensure that the `DockerClient` mock chain is fully defined. Because the Docker Java API uses a fluent builder pattern, failing to mock a single link in the chain (e.g., `withHostConfig`) will result in `NullPointerException` during test execution.

### Example: Verifying a New Lifecycle Feature
To add a test for a new container configuration (e.g., adding volume mounts), follow this pattern:

```java
@Test
void startWorkerAppliesVolumeMounts() {
    // 1. Setup the mock chain for the new command
    when(createContainerCmd.withHostConfig(any())).thenReturn(createContainerCmd);
    
    // 2. Execute the action
    runtime.startWorker(config);
    
    // 3. Verify the specific configuration was applied
    verify(createContainerCmd).withHostConfig(argThat(config -> 
        config.getBinds().length > 0
    ));
}
```
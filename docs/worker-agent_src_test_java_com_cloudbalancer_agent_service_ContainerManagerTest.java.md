# File: worker-agent/src/test/java/com/cloudbalancer/agent/service/ContainerManagerTest.java

## Overview

`ContainerManagerTest` is a JUnit 5 test suite responsible for verifying the lifecycle management of worker containers within the `worker-agent`. It ensures that the `ContainerManager` correctly interacts with the Docker daemon via the `docker-java` library to create, start, stop, and remove containers based on agent requirements.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a critical component for infrastructure orchestration, changes to this test suite or the underlying `ContainerManager` should be treated with high caution, as they directly impact the agent's ability to manage worker workloads.

## Public API

The test suite validates the following methods in `ContainerManager`:

*   **`startWorker(String workerId, int cpu, int memory, String env)`**: Verifies that the manager correctly configures environment variables, sets up container resources, and triggers the Docker start command.
*   **`stopWorker(String workerId)`**: Verifies that the manager correctly identifies the container associated with a worker ID, stops it, and removes it from the Docker host.
*   **`getActiveWorkerIds()`**: Verifies that the manager maintains an accurate registry of currently running worker containers.

## Dependencies

*   **JUnit 5 (Jupiter)**: Testing framework.
*   **Mockito**: Used for mocking the `DockerClient` and its associated command builders (`CreateContainerCmd`, `StartContainerCmd`, etc.).
*   **AssertJ**: Provides fluent assertions for verifying container states and environment variable configurations.
*   **Docker-java API**: The underlying library used to communicate with the Docker daemon.

## Usage Notes

### Testing Strategy
The suite uses deep mocking (`RETURNS_DEEP_STUBS`) to simulate the fluent API pattern of the `docker-java` library. This allows the tests to verify complex chains of Docker commands without requiring a running Docker daemon.

### Common Pitfalls
1.  **Mocking Fluent APIs**: When adding new tests, ensure that all steps in the Docker command chain (e.g., `createContainerCmd` -> `withName` -> `withEnv` -> `exec`) are mocked. Failure to mock a step in the chain will result in a `NullPointerException`.
2.  **State Management**: The `ContainerManager` maintains internal state regarding which workers it is tracking. Tests must ensure that `startWorker` is called before `stopWorker` to simulate a realistic lifecycle, or the `stopWorker` logic will correctly skip the operation (as seen in `stopWorkerSkipsWhenNotTracked`).
3.  **Environment Variable Verification**: When testing configuration injection, use `ArgumentCaptor` to inspect the list of environment variables passed to `withEnv()`. This is critical for ensuring that internal network addresses (like Kafka or Dispatcher URLs) are correctly propagated to worker containers.

### Example: Verifying Environment Configuration
To verify that a new configuration parameter is correctly passed to a container, use the following pattern:

```java
@Test
void verifyNewConfigInjection() {
    // 1. Setup the mock for the create command
    var createCmd = mock(CreateContainerCmd.class, RETURNS_DEEP_STUBS);
    when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
    
    // 2. Execute the action
    containerManager.startWorker("worker-1", 1, 1024, "VAR=VAL");
    
    // 3. Capture and assert
    var captor = ArgumentCaptor.forClass(List.class);
    verify(createCmd).withEnv(captor.capture());
    assertThat(captor.getValue()).contains("EXPECTED_VAR=EXPECTED_VAL");
}
```
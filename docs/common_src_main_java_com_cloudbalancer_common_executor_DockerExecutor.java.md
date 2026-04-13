# File: common/src/main/java/com/cloudbalancer/common/executor/DockerExecutor.java

## Overview

The `DockerExecutor` is a critical component of the `cloudbalancer` system, responsible for executing tasks within isolated Docker containers. It acts as a bridge between the system's task scheduling logic and the Docker daemon, utilizing the `docker-java` library to manage container lifecycles, resource constraints, and security hardening.

**Note**: This file is a **HOTSPOT** within the repository, characterized by high change frequency and significant complexity. It is a high-risk area for bugs; modifications should be accompanied by rigorous testing.

## Public API

### `DockerExecutor(DockerClient dockerClient)`
Constructs a new executor instance using the provided `DockerClient` instance.

### `ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`
Executes a task based on the provided specification.
*   **Parameters**:
    *   `spec`: A map containing configuration (e.g., `image`, `command`, `environment`, `memoryLimitBytes`).
    *   `allocation`: Resource constraints for the container.
    *   `context`: Execution context including task ID and logging callbacks.
*   **Returns**: An `ExecutionResult` containing the exit code, stdout/stderr logs, and execution metadata.

### `ValidationResult validate(Map<String, Object> spec)`
Validates the task specification. Currently ensures that the `image` field is present and non-blank.

### `ResourceEstimate estimateResources(Map<String, Object> spec)`
Provides a static resource estimate for the task.

### `ExecutorCapabilities getCapabilities()`
Returns the capabilities of this executor, including security levels and resource limits.

### `ExecutorType getExecutorType()`
Returns `ExecutorType.DOCKER`.

### `void cancel(ExecutionHandle handle)`
Attempts to terminate a running container associated with the provided `ExecutionHandle`.

## Dependencies

*   **`com.github.dockerjava`**: Used for all interactions with the Docker daemon (pulling images, creating/starting containers, streaming logs).
*   **`com.cloudbalancer.common.model`**: Defines the data structures for task specifications, capabilities, and execution results.
*   **`java.util.concurrent`**: Manages thread-safe tracking of active containers via `ConcurrentHashMap`.

## Usage Notes

### Security Hardening
The `DockerExecutor` enforces security by default:
*   **`no-new-privileges`**: Prevents processes from gaining new privileges.
*   **`Capability.ALL`**: Drops all Linux capabilities by default to minimize the attack surface.
*   **Read-only Rootfs**: Can be enabled via the `readOnlyRootfs` flag in the task specification.

### Lifecycle Management
*   **Image Pulling**: The executor automatically pulls the specified image before execution, with a 120-second timeout.
*   **Cleanup**: The `finally` block ensures that containers are removed (`dockerClient.removeContainerCmd`) after execution, regardless of success or failure.
*   **Cancellation**: Tasks are tracked in a `ConcurrentHashMap`. When `cancel()` is called, the executor issues a `killContainerCmd` to the Docker daemon.

### Implementation Pitfalls
*   **Log Streaming**: The executor uses a `ResultCallback.Adapter` to stream logs in real-time. If the log volume is extremely high, this may impact memory usage.
*   **Timeouts**: Hardcoded timeouts (e.g., 300 seconds for container execution) may need adjustment for long-running tasks.
*   **Resource Limits**: While the executor supports `memoryLimitBytes`, `memorySwapBytes`, and `cpuCount`, ensure the host environment has sufficient resources to accommodate these requests, or the container creation may fail.

### Example Usage
```java
// Initialize the client
DockerClient client = DockerClientBuilder.getInstance().build();
DockerExecutor executor = new DockerExecutor(client);

// Define task
Map<String, Object> spec = Map.of(
    "image", "alpine:latest",
    "command", List.of("echo", "hello world")
);

// Execute
ExecutionResult result = executor.execute(spec, allocation, context);
if (result.exitCode() == 0) {
    System.out.println("Output: " + result.stdout());
}
```
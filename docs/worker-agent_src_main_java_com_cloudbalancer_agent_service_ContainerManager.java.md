# File: worker-agent/src/main/java/com/cloudbalancer/agent/service/ContainerManager.java

## Overview

The `ContainerManager` is a critical service within the `worker-agent` responsible for the lifecycle management of Docker-based worker containers. It acts as the bridge between the agent's control logic and the underlying Docker daemon. 

**Note:** This file is a **HOTSPOT** within the repository, ranking in the top 25% for both change frequency and complexity. It is a high-risk area for bugs; modifications should be accompanied by thorough integration testing, particularly regarding container cleanup and resource allocation.

The service handles:
*   **Startup Reconciliation**: Automatically cleaning up orphaned containers from previous agent sessions.
*   **Worker Lifecycle**: Programmatic creation, execution, and termination of worker containers with specific resource constraints (CPU/Memory).
*   **Environment Configuration**: Injecting Kafka security credentials and network settings into worker containers.

## Public API

### `startWorker(String workerId, int cpuCores, int memoryMB, String... envVars)`
Creates and starts a new Docker container for a specific worker.
*   **Parameters**:
    *   `workerId`: Unique identifier for the worker.
    *   `cpuCores`: Number of CPU cores to allocate.
    *   `memoryMB`: Memory limit in Megabytes.
    *   `envVars`: Additional environment variables to inject.
*   **Returns**: The Docker container ID.

### `stopWorker(String workerId)`
Gracefully stops and removes a container associated with a specific `workerId`. If the ID is not tracked, the operation is skipped.

### `getActiveWorkerIds()`
Returns a list of all currently tracked worker IDs managed by this instance.

## Dependencies

*   **`DockerClient`**: Used for all interactions with the Docker daemon (list, create, start, stop, remove).
*   **`AgentProperties`**: Provides configuration for Docker images, network names, and infrastructure URLs.
*   **`AgentRegistrationClient`**: Provides cached Kafka credentials and connection details required for worker authentication.

## Usage Notes

### Lifecycle Management
The `ContainerManager` uses a `ConcurrentHashMap` to track active containers. Because it relies on `docker.sock` binding, ensure the agent process has appropriate permissions to access the Docker socket on the host.

### Reconciliation
The `@PostConstruct` method `reconcileOnStartup` is triggered automatically when the Spring context initializes. It scans for any containers prefixed with `cloudbalancer-worker-` and removes them. This ensures that if an agent crashes and restarts, it starts with a clean slate.

### Implementation Pitfalls
1.  **Orphaned Containers**: If the agent process is killed via `SIGKILL`, the `workerContainers` map is lost, but the containers remain running. The `reconcileOnStartup` method mitigates this, but manual intervention may be required if the container naming convention is altered.
2.  **Resource Limits**: The `startWorker` method sets `withPrivileged(true)`. Ensure this aligns with your security policy, as it grants the worker container significant host access.
3.  **Timeout Handling**: `safeStopAndRemove` uses a 10-second timeout for stopping containers. If a worker process is stuck in a non-interruptible state, the removal may fail, leaving the container in an inconsistent state.

### Example Usage
```java
// Injecting the manager via Spring
@Autowired
private ContainerManager containerManager;

// Starting a worker with 2 cores and 1GB RAM
String containerId = containerManager.startWorker(
    "worker-123", 
    2, 
    1024, 
    "LOG_LEVEL=DEBUG"
);

// Stopping the worker later
containerManager.stopWorker("worker-123");
```
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scaling/DockerRuntime.java

## Overview

`DockerRuntime` is a critical component of the CloudBalancer infrastructure, responsible for managing the lifecycle of worker nodes deployed as Docker containers. It implements the `NodeRuntime` interface, providing the bridge between the dispatcher's scaling logic and the underlying Docker engine.

**Hotspot Warning**: This file is a high-activity component with significant complexity regarding container orchestration and state reconciliation. It is a high-risk area for bugs related to container lifecycle management, orphaned processes, and synchronization between the `WorkerRegistryService` and the Docker daemon.

## Public API

The class implements the `NodeRuntime` interface. Key methods include:

*   **`startWorker(WorkerConfig config)`**: Provisions and starts a new Docker container based on the provided configuration. It injects environment variables, sets resource limits (CPU/Memory), and registers the worker with the `WorkerRegistryService`.
*   **`drainAndStop(String workerId, int drainTimeSeconds)`**: Initiates a graceful shutdown sequence. It publishes a `DrainCommand` to the event bus and schedules an automated `stopWorker` call after the specified duration.
*   **`stopWorker(String workerId)`**: Immediately terminates and removes the Docker container associated with the given `workerId` and marks the worker as `DEAD` in the registry.
*   **`listWorkers()`**: Returns a list of `WorkerInfo` objects for all currently tracked Docker containers.
*   **`getWorkerInfo(String workerId)`**: Retrieves status information for a specific worker from the registry.

## Dependencies

*   **`DockerClient`**: The `docker-java` client used to communicate with the Docker daemon.
*   **`WorkerRegistryService`**: Maintains the source of truth for worker health states and metadata.
*   **`EventPublisher`**: Used to broadcast lifecycle events (e.g., `WorkerRegisteredEvent`) and commands (e.g., `DrainCommand`) to the cluster.
*   **`ScalingProperties`**: Configuration provider for Docker-specific settings like image names, network configurations, and default drain timeouts.

## Usage Notes

### Startup Reconciliation
Upon application startup, the `@PostConstruct` annotated `reconcileOnStartup` method executes. It performs the following safety checks:
1.  Lists all containers matching the `cloudbalancer-worker-` prefix.
2.  If a container exists but is not registered or is marked `DEAD`, it is removed to prevent resource leaks.
3.  If a worker is in a `DRAINING` state, it calculates the remaining time and re-schedules the `stopWorker` task to ensure the worker is cleaned up even if the dispatcher was restarted during the drain period.

### Graceful Shutdown
The `drainAndStop` method is the preferred way to decommission workers. It relies on a `ScheduledExecutorService` (`drainScheduler`) to enforce the timeout. 
*   **Pitfall**: If the dispatcher process is killed abruptly, the `drainScheduler` tasks will be lost. The `reconcileOnStartup` method is designed to mitigate this by re-evaluating the drain state upon the next boot.

### Resource Constraints
The `startWorker` method enforces strict resource isolation:
*   **CPU**: Configured via `withNanoCPUs`.
*   **Memory**: Configured via `withMemory`.
*   **Privileges**: Containers are started in `privileged` mode, which is required for certain internal operations (e.g., mounting the Docker socket for nested container management). Ensure the host environment is secured accordingly.

### Example: Starting a Worker
```java
WorkerConfig config = new WorkerConfig("worker-01", 2, 2048, 10000, List.of("java"), Map.of());
boolean success = dockerRuntime.startWorker(config);
if (success) {
    log.info("Worker deployed successfully");
}
```

### Example: Initiating Drain
```java
// Gracefully stop worker-01 after 60 seconds
dockerRuntime.drainAndStop("worker-01", 60);
```
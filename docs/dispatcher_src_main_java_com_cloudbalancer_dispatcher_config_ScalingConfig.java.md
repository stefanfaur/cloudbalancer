# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/config/ScalingConfig.java

## Overview

The `ScalingConfig` class is a Spring `@Configuration` component responsible for defining the infrastructure and runtime beans required for the cloud balancer's scaling operations. It manages the lifecycle of Docker-based and Agent-based node runtimes, allowing the system to switch between local container orchestration and distributed agent-based scaling based on application properties.

## Public API

### `dockerClient()`
*   **Returns**: `DockerClient`
*   **Description**: Configures and provides a `DockerClient` instance using the local Unix socket (`/var/run/docker.sock`).
*   **Condition**: Active when `cloudbalancer.dispatcher.scaling.runtime-mode` is set to `DOCKER` (default).

### `dockerNodeRuntime(DockerClient, ScalingProperties, WorkerRegistryService, EventPublisher)`
*   **Returns**: `NodeRuntime`
*   **Description**: Initializes a `DockerRuntime` instance for single-host container management.
*   **Condition**: Active when `cloudbalancer.dispatcher.scaling.runtime-mode` is set to `DOCKER`.

### `agentNodeRuntime(KafkaTemplate, AgentRegistry, PendingWorkerTracker)`
*   **Returns**: `NodeRuntime`
*   **Description**: Initializes an `AgentRuntime` instance for distributed scaling operations.
*   **Condition**: Active when `cloudbalancer.dispatcher.scaling.runtime-mode` is set to `AGENT`.

## Dependencies

*   **Spring Framework**: Uses `@Configuration`, `@Bean`, and `@ConditionalOnProperty` for dependency injection and conditional bean registration.
*   **Docker Java API**: Utilizes `com.github.dockerjava` for interacting with the Docker daemon.
*   **Cloud Balancer Common**: Depends on `com.cloudbalancer.common.runtime.NodeRuntime` for the abstraction of node management.
*   **Internal Modules**:
    *   `com.cloudbalancer.dispatcher.scaling`: Provides `AgentRegistry`, `AgentRuntime`, `DockerRuntime`, and `PendingWorkerTracker`.
    *   `com.cloudbalancer.dispatcher.service`: Provides `WorkerRegistryService`.
    *   `com.cloudbalancer.dispatcher.kafka`: Provides `EventPublisher`.

## Usage Notes

*   **Runtime Selection**: The scaling mode is controlled via the `cloudbalancer.dispatcher.scaling.runtime-mode` property.
    *   Set to `DOCKER` (default) to enable local container management.
    *   Set to `AGENT` to enable distributed scaling via Kafka.
*   **Docker Prerequisites**: When using the `DOCKER` runtime mode, the application must have read/write access to `/var/run/docker.sock` on the host machine.
*   **Logging**: The configuration logs the selected runtime mode at startup to assist in debugging environment configuration issues.
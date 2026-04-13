# File: worker-agent/src/main/java/com/cloudbalancer/agent/config/AgentProperties.java

## Overview

`AgentProperties` is a configuration class that manages the settings for the `worker-agent` component. It uses Spring Boot's `@ConfigurationProperties` to bind external configuration (typically from `application.yml` or environment variables) prefixed with `cloudbalancer.agent` to Java objects.

This class defines the identity, resource capacity, and connectivity parameters for the agent, as well as specific configuration for Docker-based task execution.

## Public API

### AgentProperties
The main configuration bean.

*   **`getId()` / `setId(String)`**: Unique identifier for the agent. Defaults to `agent-1`.
*   **`getHostname()` / `setHostname(String)`**: The network hostname of the agent. Defaults to `localhost`.
*   **`getTotalCpuCores()` / `setTotalCpuCores(double)`**: Total CPU capacity available to the agent. Defaults to `8.0`.
*   **`getTotalMemoryMb()` / `setTotalMemoryMb(long)`**: Total memory capacity in MB. Defaults to `16384`.
*   **`getSupportedExecutors()` / `setSupportedExecutors(Set<ExecutorType>)`**: A set of supported execution modes (e.g., `SIMULATED`, `SHELL`, `DOCKER`).
*   **`getHeartbeatIntervalMs()` / `setHeartbeatIntervalMs(long)`**: Frequency of heartbeat signals sent to the dispatcher in milliseconds. Defaults to `10000`.
*   **`getRegistrationToken()` / `setRegistrationToken(String)`**: Security token used for authenticating the agent with the dispatcher.
*   **`getDispatcherUrl()` / `setDispatcherUrl(String)`**: The base URL of the dispatcher service. Defaults to `http://dispatcher:8080`.
*   **`getDocker()` / `setDocker(DockerProperties)`**: Accessor for nested Docker configuration.

### DockerProperties
Nested configuration class for Docker-specific settings.

*   **`getHost()` / `setHost(String)`**: Docker daemon socket or TCP host. Defaults to `unix:///var/run/docker.sock`.
*   **`getWorkerImage()` / `setWorkerImage(String)`**: The Docker image name used for spawning worker containers. Defaults to `docker-worker`.
*   **`getNetworkName()` / `setNetworkName(String)`**: The Docker network to attach worker containers to. Defaults to `docker_default`.
*   **`getKafkaBootstrapInternal()` / `setKafkaBootstrapInternal(String)`**: Internal Kafka bootstrap server address for worker communication. Defaults to `kafka:29092`.

## Dependencies

*   **`com.cloudbalancer.common.model.ExecutorType`**: Used to define the supported execution strategies.
*   **`org.springframework.boot.context.properties.ConfigurationProperties`**: Provides the binding mechanism for external configuration.
*   **`org.springframework.stereotype.Component`**: Marks the class as a Spring-managed bean.

## Usage Notes

*   **Configuration Binding**: To override these properties, define them in your `application.yml` file under the `cloudbalancer.agent` namespace. For example:
    ```yaml
    cloudbalancer:
      agent:
        id: worker-node-01
        total-cpu-cores: 16
        dispatcher-url: http://central-dispatcher:8080
    ```
*   **Registration**: The `registrationToken` is mandatory for successful communication with the dispatcher in production environments. Ensure this is injected via environment variables or secure configuration management.
*   **Docker Integration**: If `ExecutorType.DOCKER` is included in `supportedExecutors`, ensure the `DockerProperties` are correctly configured to match the environment where the agent is running, particularly the `host` and `networkName`.
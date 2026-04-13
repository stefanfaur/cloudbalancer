# File: worker-agent/src/main/java/com/cloudbalancer/agent/config/AgentConfig.java

## Overview

The `AgentConfig` class is a Spring `@Configuration` component responsible for defining and managing the lifecycle of core beans required by the `worker-agent`. It centralizes the instantiation of essential services, including JSON serialization utilities and container orchestration management, ensuring they are available for dependency injection throughout the application context.

## Public API

### `AgentConfig`
The main configuration class annotated with `@Configuration`.

### `objectMapper()`
*   **Return Type**: `ObjectMapper`
*   **Description**: Provides a singleton instance of Jackson's `ObjectMapper` for JSON data binding and serialization tasks within the agent.

### `containerManager(DockerClient, AgentProperties, AgentRegistrationClient)`
*   **Parameters**:
    *   `DockerClient`: The client interface for interacting with the local Docker daemon.
    *   `AgentProperties`: Configuration properties specific to the agent.
    *   `AgentRegistrationClient`: The client responsible for registering the agent with the control plane.
*   **Return Type**: `ContainerManager`
*   **Description**: Initializes and returns a `ContainerManager` instance, which orchestrates container lifecycle events based on the provided configuration and registration status.

## Dependencies

*   **Spring Framework**: `org.springframework.context.annotation` (for `@Configuration` and `@Bean` support).
*   **Jackson Databind**: `com.fasterxml.jackson.databind.ObjectMapper` (for JSON processing).
*   **Docker Java**: `com.github.dockerjava.api.DockerClient` (for container management).
*   **Internal Components**:
    *   `com.cloudbalancer.agent.service.ContainerManager`
    *   `com.cloudbalancer.agent.registration.AgentRegistrationClient`

## Usage Notes

*   This class is automatically scanned by the Spring container during application startup.
*   The `ContainerManager` bean relies on the presence of a `DockerClient` bean in the application context; ensure that the Docker environment is correctly configured and accessible to the agent before startup.
*   The `ObjectMapper` provided here is a default instance; if specific serialization features (e.g., custom modules, date formats) are required, they should be configured within this method.
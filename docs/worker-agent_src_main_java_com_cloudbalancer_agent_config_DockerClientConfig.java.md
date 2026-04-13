# File: worker-agent/src/main/java/com/cloudbalancer/agent/config/DockerClientConfig.java

## Overview

The `DockerClientConfig` class is a Spring `@Configuration` component responsible for initializing and providing the `DockerClient` bean to the `worker-agent` application context. It leverages the `docker-java` library to establish a connection to the Docker daemon, enabling the agent to manage containers, images, and other Docker resources.

## Public API

### `DockerClientConfig` (Class)
A configuration class that registers the `DockerClient` bean.

### `dockerClient(AgentProperties props)` (Method)
- **Description**: Creates and configures a `DockerClient` instance using properties defined in `AgentProperties`.
- **Parameters**: 
    - `props`: An instance of `AgentProperties` containing the Docker host configuration.
- **Returns**: A fully configured `DockerClient` instance ready for injection into other services.

## Dependencies

- `com.github.dockerjava:docker-java-api`: Provides the core Docker API interfaces.
- `com.github.dockerjava:docker-java-core`: Provides the core implementation logic for the Docker client.
- `com.github.dockerjava:docker-java-transport-zerodep`: Provides the zero-dependency HTTP client implementation for Docker communication.
- `org.springframework:spring-context`: Provides the `@Configuration` and `@Bean` annotations for Spring dependency injection.
- `com.cloudbalancer.agent.config.AgentProperties`: Used to retrieve externalized configuration settings for the Docker host.

## Usage Notes

- **Configuration**: The client is configured using `DefaultDockerClientConfig`, which automatically detects environment variables or system properties if specific values are not provided via `AgentProperties`.
- **Transport**: This implementation uses `ZerodepDockerHttpClient`, which is a lightweight, zero-dependency HTTP client suitable for most standard Docker daemon interactions.
- **Injection**: To use the Docker client in other components, simply inject it via constructor injection:
  ```java
  @Autowired
  private DockerClient dockerClient;
  ```
- **Requirements**: Ensure that the Docker daemon is accessible at the host address specified in `AgentProperties` (e.g., `unix:///var/run/docker.sock` or `tcp://localhost:2375`).
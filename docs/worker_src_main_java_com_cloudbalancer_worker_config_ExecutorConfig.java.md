# File: worker/src/main/java/com/cloudbalancer/worker/config/ExecutorConfig.java

## Overview

`ExecutorConfig` is a Spring `@Configuration` class responsible for initializing and providing the available `TaskExecutor` implementations for the worker node. It dynamically constructs a list of supported executors based on the `WorkerProperties` configuration, enabling the system to support various execution environments such as Docker, Python, Shell, and Simulated environments.

## Public API

### `taskExecutors`
```java
@Bean
public List<TaskExecutor> taskExecutors(WorkerProperties props)
```
- **Description**: A Spring `@Bean` method that instantiates and returns a list of `TaskExecutor` objects.
- **Parameters**: 
    - `props`: The `WorkerProperties` object containing the configuration for enabled executor types and their specific settings (e.g., shell command restrictions, Docker host).
- **Returns**: A `List<TaskExecutor>` containing the configured execution engines.

### `createDockerClient`
```java
private DockerClient createDockerClient(WorkerProperties props)
```
- **Description**: A private helper method that initializes a `DockerClient` instance using the `docker-java` library.
- **Parameters**:
    - `props`: The `WorkerProperties` object used to retrieve the Docker host configuration.
- **Returns**: A configured `DockerClient` instance ready for use by the `DockerExecutor`.

## Dependencies

- **Spring Framework**: Used for dependency injection and configuration management (`@Configuration`, `@Bean`).
- **docker-java**: Used for interacting with the Docker daemon (`DockerClient`, `DefaultDockerClientConfig`, `ZerodepDockerHttpClient`).
- **com.cloudbalancer.common**: Provides the base `TaskExecutor` interface and concrete implementations (`DockerExecutor`, `PythonExecutor`, `ShellExecutor`, `SimulatedExecutor`).
- **WorkerProperties**: Provides the configuration data required to instantiate the executors.

## Usage Notes

- **Dynamic Configuration**: The list of executors is determined at runtime based on the `ExecutorType` values defined in `WorkerProperties`. If an executor type is not included in the properties, it will not be instantiated.
- **Docker Integration**: The `createDockerClient` method uses the `ZerodepDockerHttpClient`, which is a lightweight, zero-dependency HTTP client for Docker. Ensure that the Docker host specified in `WorkerProperties` is reachable from the worker node.
- **Security**: When using the `ShellExecutor`, ensure that the `blockedCommands` list in `WorkerProperties` is correctly configured to prevent unauthorized system access.
- **Extensibility**: To add a new execution type, update the `ExecutorType` enum in the common module and add the corresponding case logic within the `taskExecutors` switch statement.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/test/TestContainersConfig.java

## Overview

`TestContainersConfig` is a Spring `@TestConfiguration` class that provides a standardized, ephemeral infrastructure environment for integration tests within the `dispatcher` module. It leverages [Testcontainers](https://www.testcontainers.org/) to manage lifecycle-managed instances of PostgreSQL and Kafka, while providing a mocked `DockerClient` to simulate container orchestration interactions without requiring a live Docker daemon for specific service logic.

## Public API

### Beans
*   **`postgresContainer()`**: Returns a configured `PostgreSQLContainer` instance using the `timescale/timescaledb:latest-pg16` image. Annotated with `@ServiceConnection` to automatically configure Spring Boot's data source properties.
*   **`kafkaContainer()`**: Returns a configured `KafkaContainer` instance using the `apache/kafka:3.9.0` image. Annotated with `@ServiceConnection` to automatically configure Spring Boot's Kafka properties.
*   **`dockerClient()`**: Returns a `@Primary` mock `DockerClient` object. This mock is pre-configured with Mockito stubs for common container lifecycle operations, including listing, creating, starting, stopping, and removing containers.

## Dependencies

*   **Spring Boot Test**: Provides `@TestConfiguration` and `@ServiceConnection` for seamless integration with the Spring application context.
*   **Testcontainers**: Used for orchestrating the PostgreSQL and Kafka containers.
*   **Docker Java API**: Used for the `DockerClient` interface definition.
*   **Mockito**: Used to stub the `DockerClient` and its associated command builders (`ListContainersCmd`, `CreateContainerCmd`, etc.).

## Usage Notes

*   **Lifecycle Management**: The PostgreSQL and Kafka containers are started statically when the class is loaded. This ensures that infrastructure is available immediately upon the initialization of the test context.
*   **Service Connection**: By using the `@ServiceConnection` annotation, developers do not need to manually define `spring.datasource.*` or `spring.kafka.bootstrap-servers` properties in `application-test.properties`; the Spring Boot Test framework automatically populates these based on the container's dynamic port mapping.
*   **Mocking Strategy**: The `dockerClient()` bean is intended for testing components that interact with the Docker API. Because it returns a Mockito mock, any interactions with the client in the application code must be verified or stubbed within the specific test class using standard Mockito syntax.
*   **Environment**: Ensure the host machine running the tests has a compatible Docker environment (or Testcontainers-supported equivalent) to successfully launch the PostgreSQL and Kafka containers.
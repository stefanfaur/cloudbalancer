# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/test/TestContainersConfig.java

## Overview

`TestContainersConfig` is a Spring `@TestConfiguration` class designed to provide ephemeral, containerized infrastructure for integration testing within the `metrics-aggregator` module. It leverages the Testcontainers library to manage the lifecycle of PostgreSQL and Apache Kafka instances, ensuring that tests run against real database and messaging environments rather than mocks.

## Public API

### `TestContainersConfig` (Class)
The configuration class is annotated with `@TestConfiguration(proxyBeanMethods = false)`, allowing it to be imported into integration tests to automatically inject container connection details.

### `postgresContainer()` (Method)
*   **Returns**: `PostgreSQLContainer<?>`
*   **Description**: Provides a bean for the PostgreSQL instance running `timescale/timescaledb:latest-pg16`. It is annotated with `@ServiceConnection`, which automatically configures the Spring application's data source properties (JDBC URL, username, password) to match the container.

### `kafkaContainer()` (Method)
*   **Returns**: `KafkaContainer`
*   **Description**: Provides a bean for the Apache Kafka instance running `apache/kafka:3.9.0`. It is annotated with `@ServiceConnection`, which automatically updates the Spring Kafka bootstrap server properties to point to the containerized broker.

## Dependencies

*   **Spring Boot Test**: `org.springframework.boot.test.context.TestConfiguration`, `org.springframework.boot.testcontainers.service.connection.ServiceConnection`
*   **Testcontainers**: `org.testcontainers.containers.PostgreSQLContainer`, `org.testcontainers.kafka.KafkaContainer`, `org.testcontainers.utility.DockerImageName`

## Usage Notes

*   **Automatic Lifecycle**: The containers are started in a `static` block, ensuring they are initialized once per test suite execution.
*   **Service Connection**: The use of `@ServiceConnection` eliminates the need for manual property overrides in `application-test.properties` or `application-test.yml`. Spring Boot automatically detects these beans and maps the connection details to the relevant application properties.
*   **Environment Requirements**: This class requires a Docker-compatible environment (e.g., Docker Desktop, OrbStack, or a remote Docker daemon) to be available on the host machine running the tests.
*   **Integration**: To use this configuration in a test class, annotate the test with `@Import(TestContainersConfig.class)`.
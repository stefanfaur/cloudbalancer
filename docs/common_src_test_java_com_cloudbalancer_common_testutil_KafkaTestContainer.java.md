# File: common/src/test/java/com/cloudbalancer/common/testutil/KafkaTestContainer.java

## Overview

`KafkaTestContainer` is a utility class designed to provide a standardized, ephemeral Apache Kafka instance for integration testing. It leverages the [Testcontainers](https://www.testcontainers.org/) library to spin up a Docker container running `apache/kafka:3.9.0`.

This utility is intended to be used in test suites where a real Kafka broker is required to validate message production, consumption, or stream processing logic.

## Public API

### `KafkaTestContainer` (Class)
The main entry point for accessing the Kafka test infrastructure. It is defined as `final` with a private constructor to prevent instantiation, acting as a static singleton wrapper for the container lifecycle.

### `getBootstrapServers()` (Method)
Returns the connection string (e.g., `PLAINTEXT://localhost:32768`) required by Kafka clients to connect to the ephemeral broker.

*   **Returns**: `String` - The bootstrap server address.

## Dependencies

*   **Testcontainers Kafka**: `org.testcontainers.kafka.KafkaContainer` (via `testcontainers-kafka` dependency).

## Usage Notes

*   **Lifecycle Management**: The container is started automatically via a `static` initializer block when the class is first loaded. This ensures the broker is ready before any tests attempt to connect.
*   **Configuration Workaround**: The container is explicitly configured with `KAFKA_LISTENERS` to override default behavior. This is a necessary workaround for Kafka 3.9.0 compatibility issues with Testcontainers 2.0.x, specifically regarding how `0.0.0.0` is handled in advertised listeners.
*   **Integration**: This utility is typically referenced by Spring `@TestConfiguration` classes (such as those found in the `metrics-aggregator` or `dispatcher` modules) to inject the dynamic bootstrap server URL into the application context via property overrides.
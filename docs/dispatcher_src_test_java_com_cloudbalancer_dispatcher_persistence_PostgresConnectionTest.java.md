# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/PostgresConnectionTest.java

## Overview

`PostgresConnectionTest` is an integration test class located in the `dispatcher` module. It serves as a foundational connectivity test to ensure that the application can successfully establish a connection to a PostgreSQL database instance. It utilizes the Testcontainers framework to spin up an ephemeral PostgreSQL container, providing a reliable environment for verifying database driver compatibility and basic SQL execution capabilities.

## Public API

### `PostgresConnectionTest` (Class)
- **Annotations**: `@Testcontainers`
- **Purpose**: Orchestrates the lifecycle of a PostgreSQL container for integration testing.

### `postgresAcceptsConnections` (Method)
- **Signature**: `void postgresAcceptsConnections() throws Exception`
- **Purpose**: Verifies that a JDBC connection can be established with the containerized database and that a simple `SELECT 1` query returns the expected result.

## Dependencies

- **JUnit 5**: Used for test lifecycle management and assertions.
- **Testcontainers (PostgreSQL)**: Provides the `PostgreSQLContainer` implementation to manage the lifecycle of the Docker-based database instance.
- **AssertJ**: Used for fluent assertion syntax.
- **Java SQL API**: Standard `java.sql` package for managing database connections and executing queries.

## Usage Notes

- **Container Configuration**: The test uses `postgres:16-alpine` as the base image. The database is initialized with the name `cloudbalancer`, username `postgres`, and password `postgres`.
- **Environment Requirements**: This test requires a Docker-compatible environment to be running on the host machine (e.g., Docker Desktop, Rancher Desktop, or a local Docker daemon) to instantiate the `PostgreSQLContainer`.
- **Execution**: As an integration test, it is intended to be run as part of the standard build lifecycle (e.g., `mvn test` or `gradle test`) to validate the infrastructure configuration before running more complex persistence tests.
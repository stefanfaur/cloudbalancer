# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/MetricsAggregatorApplicationTest.java

## Overview

`MetricsAggregatorApplicationTest` is an integration test suite for the `metrics-aggregator` module. It validates the Spring Boot application context initialization and verifies that the database schema, managed by Flyway, is correctly provisioned with the required TimescaleDB hypertable configurations.

## Public API

### `MetricsAggregatorApplicationTest`

The test class uses `@SpringBootTest` to load the full application context and `@Import(TestContainersConfig.class)` to provide a containerized PostgreSQL/TimescaleDB instance for testing.

#### Methods

*   **`contextLoads()`**: Verifies that the Spring `ApplicationContext` initializes successfully without errors.
*   **`flywayMigrationCreatesAllTables()`**: Queries the `information_schema` to ensure that the `worker_metrics`, `worker_heartbeats`, and `task_metrics` tables have been created by the database migration scripts.
*   **`workerMetricsIsTimescaleHypertable()`**: Queries the `timescaledb_information` schema to confirm that the `worker_metrics` table is correctly configured as a TimescaleDB hypertable.
*   **`workerHeartbeatsIsTimescaleHypertable()`**: Queries the `timescaledb_information` schema to confirm that the `worker_heartbeats` table is correctly configured as a TimescaleDB hypertable.

## Dependencies

*   **JUnit 5**: Used for test execution and assertions.
*   **Spring Boot Test**: Provides the testing framework for Spring application contexts.
*   **TestContainers**: Orchestrates the ephemeral database environment via `TestContainersConfig`.
*   **Spring JDBC**: Used to execute raw SQL queries against the test database to verify schema state.
*   **AssertJ**: Used for fluent assertion syntax.

## Usage Notes

*   **Infrastructure Requirements**: This test requires a Docker-compatible environment to spin up the TimescaleDB container defined in `TestContainersConfig`.
*   **Database Schema**: The tests assume the existence of a `metrics` schema. If schema names or table structures change in the migration files, these tests must be updated to reflect the new database state.
*   **Execution**: These tests are intended to run as part of the standard Maven/Gradle build lifecycle to ensure that changes to database migrations do not break the core infrastructure requirements of the application.
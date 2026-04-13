# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/FlywayMigrationTest.java

## Overview

`FlywayMigrationTest` is an integration test class designed to verify the integrity of the database schema migration process managed by Flyway. It ensures that the application's database schema is correctly initialized and that all required tables are present in the `public` schema after the migration scripts have been executed.

## Public API

### `FlywayMigrationTest`
The test class utilizes the Spring Boot testing framework to bootstrap the application context and perform database validation.

*   **`allTablesCreated()`**: A test method that queries the `information_schema.tables` metadata to confirm the existence of the following tables:
    *   `users`
    *   `refresh_tokens`
    *   `tasks`
    *   `workers`
    *   `scheduling_config`
    *   `flyway_schema_history`

## Dependencies

*   **Spring Boot Test**: Used for context loading (`@SpringBootTest`).
*   **TestContainers**: Imported via `TestContainersConfig` to provide a transient, containerized database environment for testing.
*   **JdbcTemplate**: Used to execute raw SQL queries against the database to verify schema state.
*   **AssertJ**: Used for fluent assertions to validate the presence of expected tables.

## Usage Notes

*   **Environment Requirements**: This test requires a running Docker environment (via TestContainers) to spin up the database instance.
*   **Schema Validation**: The test specifically checks the `public` schema. If new tables are added to the application schema, they must be manually added to the `assertThat` list in the `allTablesCreated` method to ensure the test remains accurate.
*   **Execution**: This test is intended to run as part of the standard CI/CD pipeline to prevent regressions in database migration scripts.
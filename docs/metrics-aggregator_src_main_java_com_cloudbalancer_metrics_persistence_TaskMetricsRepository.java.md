# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/persistence/TaskMetricsRepository.java

## Overview

`TaskMetricsRepository` is a Spring Data JPA repository interface responsible for the persistence layer operations related to task execution telemetry. It provides specialized query methods to retrieve historical task completion data, which is essential for calculating system throughput and performance averages within the `cloudbalancer` ecosystem.

## Public API

### `TaskMetricsRepository`
The interface extends `JpaRepository<TaskMetricsRecord, UUID>`, inheriting standard CRUD operations for `TaskMetricsRecord` entities.

#### `countCompletedSince(Instant since)`
*   **Description**: Executes a JPQL query to count the total number of tasks that have reached a completed state at or after the specified `Instant`.
*   **Parameters**: `since` (java.time.Instant) - The starting point of the time window.
*   **Returns**: `long` - The count of completed tasks.

#### `findCompletedSince(Instant since)`
*   **Description**: Executes a JPQL query to retrieve a list of all `TaskMetricsRecord` entities that were completed at or after the specified `Instant`.
*   **Parameters**: `since` (java.time.Instant) - The starting point of the time window.
*   **Returns**: `List<TaskMetricsRecord>` - A collection of records matching the criteria.

## Dependencies

*   `org.springframework.data.jpa.repository.JpaRepository`: Base interface for JPA-based repositories.
*   `org.springframework.data.jpa.repository.Query`: Annotation used to define custom JPQL queries.
*   `org.springframework.data.repository.query.Param`: Annotation used to bind method parameters to query placeholders.
*   `java.time.Instant`: Used for precise temporal filtering of metrics.
*   `java.util.List`: Collection type for returning multiple metric records.
*   `java.util.UUID`: Primary key type for `TaskMetricsRecord`.

## Usage Notes

*   **Performance**: Both methods rely on the `completedAt` field. Ensure that the underlying database table has an index on the `completedAt` column to prevent full table scans during high-frequency metric aggregation.
*   **Throughput Calculation**: Use `countCompletedSince` when you only require a scalar value for dashboard widgets or monitoring alerts to reduce memory overhead.
*   **Average Calculation**: Use `findCompletedSince` when you need to access individual record fields (e.g., execution duration, resource usage) to perform statistical analysis or calculate averages.
*   **Data Integrity**: This repository assumes that `TaskMetricsRecord` entities are correctly populated with a non-null `completedAt` timestamp upon task completion.
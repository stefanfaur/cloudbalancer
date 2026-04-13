# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/persistence/WorkerMetricsRepository.java

## Overview

The `WorkerMetricsRepository` is a Spring Data JPA repository interface responsible for the persistence layer operations of worker performance metrics. It provides specialized data access methods to query the `metrics.worker_metrics` table, enabling the retrieval of both current state snapshots and historical time-series data for worker nodes.

## Public API

### `findLatestPerWorker()`
Retrieves the most recent metrics record for every registered worker.
*   **Implementation**: Uses a native PostgreSQL `DISTINCT ON (worker_id)` query to ensure exactly one record per worker is returned, ordered by the latest timestamp.
*   **Returns**: A `List<WorkerMetricsRecord>` containing the latest state for all workers.

### `findByWorkerIdAndTimeRange(String workerId, Instant from, Instant to)`
Retrieves raw metrics history for a specific worker within a defined temporal window.
*   **Parameters**:
    *   `workerId`: The unique identifier of the worker.
    *   `from`: The start of the time range (`Instant`).
    *   `to`: The end of the time range (`Instant`).
*   **Returns**: A `List<WorkerMetricsRecord>` ordered chronologically by the `reported_at` field.

## Dependencies

*   `org.springframework.data.jpa.repository.JpaRepository`: Provides standard CRUD operations.
*   `org.springframework.data.jpa.repository.Query`: Used for defining custom native SQL queries.
*   `org.springframework.data.repository.query.Param`: Used for binding method parameters to native query placeholders.
*   `java.time.Instant`: Used for precise timestamp handling in time-range queries.
*   `java.util.List`: Used as the return type for collection-based queries.

## Usage Notes

*   **Native Queries**: This repository utilizes `nativeQuery = true`. Ensure that the underlying database schema matches the `metrics.worker_metrics` table structure expected by the queries.
*   **PostgreSQL Specificity**: The `findLatestPerWorker` method relies on the `DISTINCT ON` syntax, which is specific to PostgreSQL. This repository is not portable to other SQL dialects (e.g., MySQL, H2) without modification.
*   **Performance**: The `findLatestPerWorker` query performs a full scan of the metrics table ordered by `worker_id`. For large datasets, ensure that an appropriate index exists on `(worker_id, reported_at DESC)` to maintain query performance.
*   **Integration**: This repository is consumed by service-layer components to feed data into the `web-dashboard` and the `dispatcher` for health monitoring and load balancing decisions.
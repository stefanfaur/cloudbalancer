# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/persistence/WorkerHeartbeatRepository.java

## Overview

The `WorkerHeartbeatRepository` is a Spring Data JPA repository interface responsible for the persistence layer operations concerning worker heartbeat data. It provides an abstraction for interacting with the `metrics.worker_heartbeats` table, enabling the system to track the operational status and liveness of worker nodes within the CloudBalancer infrastructure.

## Public API

### `WorkerHeartbeatRepository`

The interface extends `JpaRepository<WorkerHeartbeatRecord, Long>`, inheriting standard CRUD operations for `WorkerHeartbeatRecord` entities.

#### `findLatestPerWorker()`

Retrieves the most recent heartbeat record for every registered worker.

*   **Signature**: `List<WorkerHeartbeatRecord> findLatestPerWorker()`
*   **Implementation**: Uses a native PostgreSQL `DISTINCT ON` query.
*   **Query Logic**: `SELECT DISTINCT ON (worker_id) * FROM metrics.worker_heartbeats ORDER BY worker_id, timestamp DESC`
*   **Returns**: A `List` of `WorkerHeartbeatRecord` objects, where each object represents the latest entry for a unique `worker_id`.

## Dependencies

*   `org.springframework.data.jpa.repository.JpaRepository`: Provides the base framework for JPA-based repository implementations.
*   `org.springframework.data.jpa.repository.Query`: Used to define custom native SQL queries.
*   `java.util.List`: Used for returning collection results.
*   `WorkerHeartbeatRecord`: The underlying entity class (assumed) representing the database schema for heartbeats.

## Usage Notes

*   **Database Specificity**: This repository utilizes a native PostgreSQL query (`DISTINCT ON`). It is not compatible with other relational database management systems (RDBMS) without modification.
*   **Performance**: The `findLatestPerWorker` method is optimized for PostgreSQL to efficiently group by `worker_id` and select the latest timestamp. Ensure that the `worker_id` and `timestamp` columns are properly indexed in the database to maintain performance as the heartbeat table grows.
*   **Transactional Context**: As a standard Spring Data repository, calls to this interface should be executed within a transactional context if used as part of a larger business logic flow.
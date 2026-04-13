# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/service/MetricsAggregationService.java

## Overview

The `MetricsAggregationService` is a Spring-managed service responsible for aggregating and retrieving performance data across the CloudBalancer infrastructure. It acts as the primary data access layer for processing worker metrics, heartbeats, and task execution statistics.

**Note:** This file is a **HOTSPOT** within the repository, ranking in the top 25% for both change frequency and complexity. It is a high-risk area for bugs; modifications should be accompanied by thorough regression testing.

## Public API

### `getLatestPerWorker()`
Returns a list of `WorkerMetricsSnapshot` objects representing the most recent metrics reported by every active worker in the cluster.

### `getWorkerHistory(String workerId, Instant from, Instant to, int bucketMinutes)`
Retrieves historical performance data for a specific worker.
*   **`bucketMinutes` <= 1**: Returns raw, unaggregated records.
*   **`bucketMinutes` > 1**: Uses TimescaleDB's `time_bucket` function to perform server-side aggregation (averaging metrics over the specified interval).

### `getClusterMetrics()`
Computes a global snapshot of the cluster, including:
*   Average CPU usage and heap utilization.
*   Total active tasks and worker health status.
*   Recent task throughput and latency statistics (based on the last 60 seconds).

## Dependencies

The service relies on the following persistence repositories and infrastructure components:
*   **`WorkerMetricsRepository`**: Accesses raw worker performance data.
*   **`WorkerHeartbeatRepository`**: Tracks worker availability and health states.
*   **`TaskMetricsRepository`**: Provides granular task execution data.
*   **`EntityManager`**: Used for executing native SQL queries required for TimescaleDB time-bucketing.

## Usage Notes

### TimescaleDB Integration
The `getWorkerHistory` method utilizes native SQL queries to leverage TimescaleDB's time-series capabilities. When modifying the SQL string, ensure that the `bucket` alias and the order of selected columns remain compatible with the mapping helpers (`toInstant`, `toDouble`, etc.).

### Data Mapping
The service includes several private helper methods (`toInstant`, `toDouble`, `toLong`, `toInt`) to safely cast `Object[]` results from native SQL queries into Java types. These methods handle potential type mismatches (e.g., `Timestamp` vs `Instant`) and should be updated if the underlying database schema or query return types change.

### Example Usage: Fetching Aggregated History
To retrieve 5-minute average metrics for a specific worker over the last hour:

```java
Instant to = Instant.now();
Instant from = to.minus(Duration.ofHours(1));
List<WorkerMetricsBucket> history = metricsAggregationService.getWorkerHistory(
    "worker-123", 
    from, 
    to, 
    5
);
```

### Performance Considerations
*   **`getClusterMetrics`**: This method performs multiple repository calls and stream operations. In a large-scale cluster, frequent polling of this endpoint may impact database performance.
*   **Native Queries**: The use of `EntityManager.createNativeQuery` bypasses some of the safety checks provided by JPA/Hibernate. Always validate input parameters (like `workerId` and `bucketMinutes`) to prevent SQL injection or invalid interval formats.
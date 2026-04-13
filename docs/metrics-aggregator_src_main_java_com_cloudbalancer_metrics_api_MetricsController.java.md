# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/api/MetricsController.java

## Overview

The `MetricsController` is a Spring `@RestController` that serves as the primary HTTP interface for the `metrics-aggregator` service. It exposes endpoints to retrieve real-time and historical performance data for both individual workers and the overall cluster. This controller delegates data retrieval and business logic to the `MetricsAggregationService`.

## Public API

### `getLatestMetrics()`
*   **Endpoint**: `GET /api/metrics/workers`
*   **Description**: Retrieves the most recent performance snapshots for all active workers.
*   **Returns**: `List<WorkerMetricsSnapshot>`

### `getWorkerHistory(String id, String from, String to, String bucket)`
*   **Endpoint**: `GET /api/metrics/workers/{id}/history`
*   **Description**: Fetches historical metrics for a specific worker within a defined time range, aggregated into time buckets.
*   **Parameters**:
    *   `id` (Path Variable): The unique identifier of the worker.
    *   `from` (Query Param): ISO-8601 timestamp for the start of the range (defaults to 1 hour ago).
    *   `to` (Query Param): ISO-8601 timestamp for the end of the range (defaults to current time).
    *   `bucket` (Query Param): Aggregation interval (e.g., "1m", "5m", "1h"). Defaults to "1m".
*   **Returns**: `List<WorkerMetricsBucket>`

### `getClusterMetrics()`
*   **Endpoint**: `GET /api/metrics/cluster`
*   **Description**: Retrieves aggregated performance metrics for the entire cluster.
*   **Returns**: `ClusterMetrics`

## Dependencies

*   **`MetricsAggregationService`**: The service layer responsible for querying the underlying metrics storage.
*   **Spring Web MVC**: Provides the `@RestController`, `@GetMapping`, and parameter mapping annotations.
*   **Java Time API**: Used for handling ISO-8601 timestamps and duration calculations.
*   **Regex**: Used for parsing the custom bucket duration string format.

## Usage Notes

*   **Bucket Format**: The `bucket` parameter in `getWorkerHistory` follows a strict regex pattern `^(\d+)([mh])$`. 
    *   `m` denotes minutes (e.g., `15m` = 15 minutes).
    *   `h` denotes hours (e.g., `2h` = 120 minutes).
    *   If the input does not match this pattern, the system defaults to a 1-minute bucket.
*   **Time Formatting**: The `from` and `to` parameters must be valid ISO-8601 strings (e.g., `2023-10-27T10:00:00Z`).
*   **Integration**: This controller is consumed by the `web-dashboard` frontend via the `web-dashboard/src/api/workers.ts` API layer.
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/api/dto/WorkerMetricsBucket.java

## Overview

`WorkerMetricsBucket` is a Java `record` used to represent a snapshot of aggregated performance metrics for a specific worker node over a defined time interval. It serves as a Data Transfer Object (DTO) to encapsulate statistical data points, facilitating the transmission of worker health and performance telemetry within the `cloudbalancer` metrics pipeline.

## Public API

The `WorkerMetricsBucket` record exposes the following immutable fields:

*   **`bucketStart`** (`Instant`): The timestamp marking the beginning of the aggregation window.
*   **`workerId`** (`String`): The unique identifier of the worker node.
*   **`avgCpuPercent`** (`double`): The average CPU utilization percentage during the bucket interval.
*   **`avgHeapUsedMB`** (`long`): The average heap memory usage in megabytes.
*   **`avgHeapMaxMB`** (`long`): The average maximum heap memory capacity in megabytes.
*   **`avgThreadCount`** (`int`): The average number of active threads.
*   **`avgActiveTaskCount`** (`int`): The average number of tasks currently being processed.
*   **`avgCompletedTaskCount`** (`long`): The average count of successfully completed tasks.
*   **`avgFailedTaskCount`** (`long`): The average count of tasks that failed during execution.
*   **`avgExecutionDurationMs`** (`double`): The average duration of task execution in milliseconds.

## Dependencies

*   `java.time.Instant`: Used for precise temporal tracking of the metric bucket start time.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once a `WorkerMetricsBucket` is instantiated, its values cannot be modified.
*   **Aggregation Context**: This DTO is intended for use in systems that aggregate raw metrics over time windows. Ensure that the values provided represent the calculated averages for the specified `bucketStart` window to maintain data consistency.
*   **Serialization**: Being a standard Java record, it is compatible with most JSON serialization libraries (e.g., Jackson, Gson) for transport across service boundaries.
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/api/dto/WorkerMetricsSnapshot.java

## Overview

`WorkerMetricsSnapshot` is a Java `record` that serves as a data transfer object (DTO) representing the state of a specific worker node at a given point in time. It encapsulates key performance indicators and resource utilization metrics, providing a standardized format for reporting worker health and workload status to the metrics aggregation system.

## Public API

The `WorkerMetricsSnapshot` record exposes the following immutable fields:

*   **`workerId`** (`String`): Unique identifier for the worker node.
*   **`cpuUsagePercent`** (`double`): Current CPU utilization as a percentage.
*   **`heapUsedMB`** (`long`): The amount of JVM heap memory currently in use, measured in megabytes.
*   **`heapMaxMB`** (`long`): The maximum JVM heap memory capacity, measured in megabytes.
*   **`threadCount`** (`int`): The total number of active threads currently managed by the worker.
*   **`activeTaskCount`** (`int`): The number of tasks currently being processed.
*   **`completedTaskCount`** (`long`): The cumulative number of tasks successfully completed by the worker.
*   **`failedTaskCount`** (`long`): The cumulative number of tasks that resulted in failure.
*   **`avgExecutionDurationMs`** (`double`): The moving average duration of task executions, measured in milliseconds.
*   **`reportedAt`** (`Instant`): The timestamp indicating when the snapshot was generated.

## Dependencies

*   `java.time.Instant`: Used to provide precise, UTC-based timestamps for metric reporting.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once a snapshot is instantiated, its values cannot be modified, ensuring thread safety when passing metrics through the aggregation pipeline.
*   **Serialization**: This record is intended for use in serialization contexts (e.g., JSON via Jackson or similar libraries) to transmit worker telemetry across the network.
*   **Data Integrity**: Ensure that `reportedAt` is populated using a synchronized system clock to maintain accurate temporal ordering of snapshots across distributed worker nodes.
*   **Maintainer**: Primary maintenance is handled by **sfaur**.
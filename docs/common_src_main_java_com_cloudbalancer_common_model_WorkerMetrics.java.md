# File: common/src/main/java/com/cloudbalancer/common/model/WorkerMetrics.java

## Overview

The `WorkerMetrics` class is an immutable data carrier (Java Record) used to encapsulate the performance and operational state of a worker node within the CloudBalancer system. It provides a snapshot of resource utilization, task processing throughput, and system health metrics at a specific point in time.

## Public API

### `WorkerMetrics` Record

The record exposes the following fields as public accessors:

| Field | Type | Description |
| :--- | :--- | :--- |
| `cpuUsagePercent` | `double` | The current CPU utilization percentage. |
| `heapUsedMB` | `long` | The amount of JVM heap memory currently in use (in Megabytes). |
| `heapMaxMB` | `long` | The maximum available JVM heap memory (in Megabytes). |
| `threadCount` | `int` | The current number of active threads in the worker JVM. |
| `activeTaskCount` | `int` | The number of tasks currently being processed. |
| `completedTaskCount` | `long` | The cumulative number of tasks successfully completed. |
| `failedTaskCount` | `long` | The cumulative number of tasks that resulted in failure. |
| `averageExecutionDurationMs` | `double` | The average time taken to execute tasks (in milliseconds). |
| `reportedAt` | `Instant` | The timestamp indicating when these metrics were captured. |

## Dependencies

- `java.time.Instant`: Used for precise timestamping of the metrics snapshot.

## Usage Notes

- **Immutability**: As a Java Record, this class is immutable. Once a `WorkerMetrics` instance is created, its values cannot be modified.
- **Data Serialization**: This record is intended to be used for transmitting worker status across the network (e.g., via JSON serialization) or storing snapshots in a monitoring database.
- **Time Precision**: The `reportedAt` field utilizes `java.time.Instant`, ensuring UTC-based time consistency across distributed worker nodes.
- **Metric Interpretation**: 
    - `heapUsedMB` and `heapMaxMB` should be compared to determine memory pressure.
    - `completedTaskCount` and `failedTaskCount` are cumulative counters and should be used to calculate error rates over time intervals.
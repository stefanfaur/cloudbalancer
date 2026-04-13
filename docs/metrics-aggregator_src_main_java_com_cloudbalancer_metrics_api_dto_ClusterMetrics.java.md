# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/api/dto/ClusterMetrics.java

## Overview

The `ClusterMetrics` class is a Java `record` that serves as a Data Transfer Object (DTO) for representing aggregated performance and health statistics across a computing cluster. It consolidates data derived from individual worker nodes and task-level metrics, providing a high-level snapshot of the cluster's operational state.

## Public API

The `ClusterMetrics` record exposes the following immutable fields:

*   **`double avgCpuPercent`**: The average CPU utilization percentage across all active workers in the cluster.
*   **`int totalActiveTaskCount`**: The total number of tasks currently being processed across the cluster.
*   **`long totalHeapUsedMB`**: The cumulative heap memory usage across the cluster, measured in megabytes.
*   **`double throughputPerMinute`**: The aggregate rate of task completion across the cluster, measured in tasks per minute.
*   **`double avgQueueWaitMs`**: The average time (in milliseconds) tasks spend waiting in queues before execution.
*   **`double avgExecutionDurationMs`**: The average duration (in milliseconds) of task execution across the cluster.
*   **`int workerCount`**: The total number of registered workers in the cluster.
*   **`int healthyWorkerCount`**: The number of workers currently reporting a healthy status.

## Dependencies

This class is a standard Java `record` and does not depend on external libraries or frameworks. It relies solely on the Java SE platform (Java 14+).

## Usage Notes

*   **Immutability**: As a Java `record`, all fields are final and immutable. Once an instance is created, its state cannot be modified.
*   **Data Source**: This DTO is intended to be populated by the metrics aggregation service, which processes raw data from `latest-per-worker` and `task_metrics` sources.
*   **Serialization**: Being a standard DTO, it is compatible with common JSON serialization libraries (e.g., Jackson, Gson) for transmission over REST APIs or messaging queues.
*   **Primary Maintainer**: sfaur
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/persistence/TaskMetricsRecord.java

## Overview

`TaskMetricsRecord` is a JPA entity that maps to the `task_metrics` table within the `metrics` schema. It serves as the data model for storing aggregated performance telemetry for tasks processed by the CloudBalancer system. This record captures the lifecycle timestamps of a task and calculated performance metrics, enabling historical analysis of task execution efficiency.

## Public API

### Class: `TaskMetricsRecord`
The entity provides standard getter and setter methods for all fields.

| Field | Type | Description |
| :--- | :--- | :--- |
| `taskId` | `UUID` | Unique identifier for the task (Primary Key). |
| `submittedAt` | `Instant` | Timestamp when the task was initially submitted. |
| `assignedAt` | `Instant` | Timestamp when the task was assigned to a worker. |
| `startedAt` | `Instant` | Timestamp when the task execution began. |
| `completedAt` | `Instant` | Timestamp when the task execution finished. |
| `queueWaitMs` | `Long` | Calculated duration spent in the queue (ms). |
| `executionDurationMs` | `Long` | Actual time spent executing the task (ms). |
| `turnaroundMs` | `Long` | Total time from submission to completion (ms). |

### Constructors
*   `TaskMetricsRecord()`: Default constructor required for JPA entity instantiation.

## Dependencies

*   **Jakarta Persistence API**: Used for ORM mapping (`@Entity`, `@Table`, `@Id`, `@Column`).
*   **java.time.Instant**: Used for precise temporal tracking of task lifecycle events.
*   **java.util.UUID**: Used for unique identification of tasks.

## Usage Notes

*   **Persistence**: This entity is designed to be used with a Spring Data JPA repository (e.g., `TaskMetricsRepository`) to persist telemetry data to the database.
*   **Schema**: The entity is explicitly mapped to the `metrics` schema. Ensure the database user has appropriate permissions to access this schema.
*   **Calculated Fields**: While the entity provides setters for `queueWaitMs`, `executionDurationMs`, and `turnaroundMs`, these values are typically calculated by the service layer before being persisted to the database.
*   **Immutability**: While setters are provided for compatibility with JPA frameworks, these records are intended to represent historical snapshots of completed tasks and should generally be treated as immutable once persisted.
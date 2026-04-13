# Module: metrics-aggregator

## Overview

The `metrics-aggregator` module is a core component of the CloudBalancer infrastructure, designed to ingest, process, and serve real-time and historical performance telemetry. It functions as a bridge between distributed worker nodes and the monitoring dashboard, consuming high-volume event streams from Apache Kafka and persisting them into a time-series optimized database.

The module provides a secure REST API for querying cluster-wide health, individual worker performance, and task execution history. It is built on Spring Boot and leverages Spring Security with JWT-based authentication to ensure that performance data is accessed only by authorized entities.

## Public API Summary

### REST Endpoints (`MetricsController`)
*   **`GET /metrics/cluster`**: Retrieves aggregated health and performance statistics across the entire cluster.
*   **`GET /metrics/latest`**: Returns the most recent performance snapshot for all active workers.
*   **`GET /metrics/history/{workerId}`**: Fetches historical performance data for a specific worker, supporting time-range filtering and bucketed aggregation.

### Data Models (DTOs)
*   **`ClusterMetrics`**: Aggregated performance and health statistics for the cluster.
*   **`WorkerMetricsSnapshot`**: Point-in-time state of a specific worker node.
*   **`WorkerMetricsBucket`**: Aggregated performance metrics for a worker over a defined time interval.

### Security
*   **`JwtService`**: Handles JWT generation, validation, and claim extraction.
*   **`JwtAuthenticationFilter`**: Intercepts requests to enforce stateless authentication via JWT.
*   **`SecurityConfig`**: Defines the security filter chain and CORS policies.

### Persistence Layer
*   **`TaskMetricsRepository`**: Manages task execution telemetry (e.g., turnaround time, queue wait time).
*   **`WorkerMetricsRepository`**: Manages granular worker performance metrics.
*   **`WorkerHeartbeatRepository`**: Tracks worker availability and liveness signals.

## Architecture Notes

*   **Event-Driven Ingestion**: The module utilizes Kafka listeners (`HeartbeatMetricsListener`, `TaskEventsListener`, `WorkerMetricsListener`) to decouple data ingestion from the API layer, ensuring high throughput and resilience.
*   **Time-Series Optimization**: The persistence layer is designed to work with TimescaleDB (as verified by integration tests), utilizing hypertables for efficient storage and querying of time-series data.
*   **Security Architecture**: The module implements a stateless security model. Authentication is handled via a custom `JwtAuthenticationFilter`, and authorization is managed through standard Spring Security configurations.
*   **Testing Strategy**: The module employs a robust testing suite, including:
    *   **Integration Tests**: Uses `TestContainers` to spin up ephemeral Kafka and PostgreSQL/TimescaleDB instances, ensuring the entire pipeline (from Kafka event to API response) functions correctly.
    *   **Security Tests**: Validates authentication flows, including handling of expired or invalid tokens.
    *   **API Tests**: Uses `MockMvc` to verify endpoint behavior and CORS configurations.
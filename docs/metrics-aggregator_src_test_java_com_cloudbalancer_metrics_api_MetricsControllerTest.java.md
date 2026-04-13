# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/api/MetricsControllerTest.java

## Overview

`MetricsControllerTest` is a critical Spring Boot integration test suite for the `MetricsController` within the `metrics-aggregator` module. It validates the REST API endpoints responsible for retrieving real-time and historical worker metrics, cluster-wide health status, and task throughput statistics.

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it tests the core data aggregation logic for the entire cluster, any regressions here likely indicate failures in the persistence layer or the metrics calculation engine.

## Public API

The test suite validates the following endpoints:

*   `GET /api/metrics/workers`: Retrieves the latest metrics snapshot for all active workers.
*   `GET /api/metrics/workers/{id}/history`: Retrieves historical metrics for a specific worker, supporting time-range filtering and bucket-based aggregation.
*   `GET /api/metrics/cluster`: Returns an aggregated view of the cluster, including average CPU usage, total active tasks, heap utilization, and task throughput.

### Key Test Methods
*   `getLatestMetrics_returnsLatestPerWorker()`: Ensures the system correctly identifies the most recent record per worker ID.
*   `getWorkerHistory_bucketAggregation_returnsBuckets()`: Validates that TimescaleDB-style time-bucket aggregation correctly averages metrics over defined intervals.
*   `getClusterMetrics_aggregatesCorrectly()`: Verifies the mathematical accuracy of cluster-wide averages and health counts.

## Dependencies

The test suite relies on the following infrastructure and components:

*   **Spring Boot Test**: Uses `@SpringBootTest` with `RANDOM_PORT` for full context loading.
*   **TestContainers**: Uses `TestContainersConfig` to spin up ephemeral database instances (likely PostgreSQL/TimescaleDB) to ensure test isolation and environment parity.
*   **MockMvc**: Used for performing HTTP requests and verifying responses without starting a full network server.
*   **Persistence Repositories**: Directly interacts with `WorkerMetricsRepository`, `WorkerHeartbeatRepository`, and `TaskMetricsRepository` to seed test data.
*   **Security**: Integrates with `JwtService` to simulate authenticated requests (required for all endpoints).

## Usage Notes

### Test Data Seeding
The class provides several helper methods to generate consistent test records:
*   `metricsRow(...)`: Creates a `WorkerMetricsRecord` with configurable CPU, heap, and task counts.
*   `heartbeatRow(...)`: Simulates worker health status (`HEALTHY`/`UNHEALTHY`).
*   `taskRow(...)`: Generates `TaskMetricsRecord` to test throughput and latency calculations.

### Common Pitfalls
1.  **Time Truncation**: When testing time-based queries, always use `Instant.now().truncatedTo(ChronoUnit.SECONDS)` to avoid precision mismatches between Java `Instant` and database timestamps.
2.  **Database Cleanup**: The `@AfterEach` `cleanup()` method is essential. Failure to clear the repositories will cause state leakage between tests, leading to non-deterministic test failures.
3.  **Authentication**: All requests require a valid JWT. If a test fails with a `401 Unauthorized` status, ensure the `jwt()` helper is correctly included in the request header.

### Example: Adding a New Test Case
To test a new aggregation logic, follow this pattern:

```java
@Test
void getNewMetric_calculatesCorrectly() throws Exception {
    // 1. Seed data
    workerMetricsRepo.save(metricsRow("worker-1", Instant.now(), 50.0, 1024, 1));
    
    // 2. Perform request
    MvcResult result = mockMvc.perform(get("/api/metrics/new-endpoint")
                    .header("Authorization", "Bearer " + jwt()))
            .andExpect(status().isOk())
            .andReturn();
            
    // 3. Assert
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("value").asDouble()).isEqualTo(50.0);
}
```
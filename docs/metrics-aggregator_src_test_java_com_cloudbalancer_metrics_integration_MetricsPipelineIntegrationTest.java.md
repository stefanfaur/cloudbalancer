# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/integration/MetricsPipelineIntegrationTest.java

## Overview

`MetricsPipelineIntegrationTest` is a critical integration test suite for the `metrics-aggregator` component. It validates the end-to-end data pipeline, ensuring that events published to Kafka are correctly processed, persisted, and exposed via the REST API. 

**Note:** This file is a **hotspot** in the repository, characterized by high change frequency and complexity. It serves as the primary verification layer for the system's observability features. Changes to event schemas, database persistence logic, or API contracts must be validated against this suite to prevent regressions.

## Public API

The class provides a suite of `@Test` methods that simulate real-world traffic patterns:

*   **`metricsPipelineEndToEnd_publishEventAndQueryViaApi`**: Verifies that `WorkerMetricsEvent` messages are ingested from Kafka and retrievable via the `/api/metrics/workers` endpoint.
*   **`heartbeatPipelineEndToEnd_publishEventAndVerifyInDb`**: Ensures `WorkerHeartbeatEvent` messages correctly update the worker's health state in the persistence layer.
*   **`taskMetricsPipelineEndToEnd_publishEventAndQueryCluster`**: Validates the aggregation logic for cluster-wide metrics, including throughput calculations based on `TaskCompletedEvent` and heartbeat status.
*   **`timeRangeQueryEndToEnd_publishMultipleEventsAndQueryHistory`**: Tests the historical query capability, ensuring that multiple metrics events are stored and retrieved correctly within a specific time window.
*   **`multipleWorkersInClusterMetrics_publishMetricsFor3Workers`**: Confirms that the cluster aggregation logic correctly handles concurrent data from multiple worker nodes.

## Dependencies

This test suite relies on the following infrastructure and components:

*   **Spring Boot Test**: Uses `@SpringBootTest` with `RANDOM_PORT` to spin up the full application context.
*   **TestContainers**: Leverages `TestContainersConfig` to provide ephemeral, containerized instances of Kafka and the backing database.
*   **MockMvc**: Used for performing authenticated HTTP requests to the internal API.
*   **KafkaTemplate**: Used to inject test events into the Kafka message bus.
*   **Persistence Repositories**: Directly interacts with `WorkerMetricsRepository`, `WorkerHeartbeatRepository`, and `TaskMetricsRepository` to verify state changes.
*   **Awaitility**: Essential for handling the asynchronous nature of the pipeline, allowing tests to poll for expected states with configurable timeouts.

## Usage Notes

### Asynchronous Testing
Because the pipeline is asynchronous (Kafka-based), tests use `Awaitility` to poll for results. 
*   **Timeout**: Most tests are configured with a 15-second timeout. If the pipeline latency increases, these tests may become flaky.
*   **Poll Interval**: Set to 500ms to balance test speed with system load.

### Authentication
All API-facing tests require a valid JWT. The `jwt()` helper method generates a token with `Role.ADMIN` permissions using the `JwtService`. If security configurations change, ensure the `jwt()` method is updated to reflect the new authentication requirements.

### Data Cleanup
The `@AfterEach` `cleanup()` method is mandatory to ensure test isolation. It clears all metrics, heartbeats, and task records from the database. Failure to maintain this cleanup logic will lead to cross-test pollution, especially in the cluster-wide aggregation tests.

### Example: Adding a New Pipeline Test
To add a new test case for a new event type:
1.  **Define the Event**: Create the event object (e.g., `NewEvent`).
2.  **Publish**: Use `kafkaTemplate.send("topic.name", key, payload)`.
3.  **Await**: Use `await().atMost(...).untilAsserted(...)` to verify the side effect in the database or via the API.
4.  **Assert**: Use AssertJ to verify the specific fields in the retrieved JSON or database entity.

```java
// Example snippet for verifying a new event
kafkaTemplate.send("new.topic", key, jsonPayload);
await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
    var result = repository.findById(id);
    assertThat(result).isPresent();
});
```
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/kafka/WorkerMetricsListener.java

## Overview

The `WorkerMetricsListener` is a Spring `@Component` responsible for consuming worker performance and health metrics from an Apache Kafka topic. It acts as the ingestion point for the `metrics-aggregator` service, transforming raw JSON event data into persistent records stored in the system's database.

This component bridges the gap between the distributed worker nodes (which report metrics via `MetricsReporter`) and the centralized persistence layer, enabling historical analysis and monitoring of the cloud balancer infrastructure.

## Public API

### `WorkerMetricsListener`

*   **Constructor**: `WorkerMetricsListener(WorkerMetricsRepository repository)`
    *   Initializes the listener with the required `WorkerMetricsRepository` for database operations.

*   **`onWorkerMetrics(String message)`**
    *   **Annotation**: `@KafkaListener(topics = "workers.metrics", groupId = "metrics-aggregator-group")`
    *   **Description**: Listens to the `workers.metrics` Kafka topic. It deserializes incoming JSON messages into `WorkerMetricsEvent` objects and maps them to `WorkerMetricsRecord` entities for persistence.
    *   **Parameters**: 
        *   `message` (String): The raw JSON payload received from the Kafka topic.
    *   **Error Handling**: Logs an error if the message fails to deserialize due to `JsonProcessingException`.

## Dependencies

*   **`com.cloudbalancer.common.event.WorkerMetricsEvent`**: Data transfer object representing the structure of the incoming metrics.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Utility for JSON serialization/deserialization.
*   **`com.cloudbalancer.metrics.persistence.WorkerMetricsRecord`**: The database entity representing stored metrics.
*   **`com.cloudbalancer.metrics.persistence.WorkerMetricsRepository`**: Interface for performing CRUD operations on metrics data.
*   **`org.springframework.kafka`**: Provides the Kafka listener infrastructure.
*   **`com.fasterxml.jackson`**: Used for JSON processing.

## Usage Notes

*   **Kafka Configuration**: This component expects a Kafka broker to be configured in the Spring environment. It specifically listens to the `workers.metrics` topic.
*   **Consumer Group**: The listener is part of the `metrics-aggregator-group`. Ensure that this group ID is managed correctly if scaling the aggregator service to avoid duplicate processing or partition starvation.
*   **Data Mapping**: The listener performs a direct mapping from the `WorkerMetricsEvent` to `WorkerMetricsRecord`. Changes to the metrics schema in the worker nodes must be reflected in both the `WorkerMetricsEvent` DTO and the `WorkerMetricsRecord` entity to ensure data integrity.
*   **Logging**: The component uses SLF4J for logging. Successful processing is logged at the `DEBUG` level, while deserialization errors are logged at the `ERROR` level.
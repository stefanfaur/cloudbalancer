# File: worker/src/main/java/com/cloudbalancer/worker/service/MetricsReporter.java

## Overview

The `MetricsReporter` is a core service within the worker component responsible for monitoring system health and performance. It periodically collects resource utilization metrics (CPU, memory, thread counts) and task execution statistics, publishing them to the central control plane via Apache Kafka. Additionally, it broadcasts heartbeat signals to maintain worker visibility within the cluster.

The service integrates with Resilience4j's `CircuitBreaker` to ensure that network instability or Kafka unavailability does not impact the worker's primary execution threads.

## Public API

### `MetricsReporter` (Constructor)
Initializes the reporter with the necessary infrastructure components.
*   **Parameters**:
    *   `kafkaTemplate`: The Spring Kafka template for event publishing.
    *   `properties`: Configuration properties for the worker (e.g., worker ID).
    *   `taskExecutionService`: Service providing real-time task statistics.
    *   `circuitBreaker`: A Resilience4j `CircuitBreaker` instance (qualified as `workerResultProducerCircuitBreaker`) to protect Kafka communication.

### `publishMetrics`
A scheduled task that gathers system and task metrics and publishes them to the `workers.metrics` Kafka topic.
*   **Interval**: Configurable via `cloudbalancer.worker.metrics-interval-ms` (default: 5000ms).

### `publishHeartbeat`
A scheduled task that broadcasts a `WorkerHeartbeatEvent` to the `workers.heartbeat` Kafka topic to signal that the worker is alive and healthy.
*   **Interval**: Configurable via `cloudbalancer.worker.heartbeat-interval-ms` (default: 10000ms).

## Dependencies

*   **Spring Framework**: Uses `@Service` for component scanning, `@Scheduled` for periodic tasks, and `KafkaTemplate` for messaging.
*   **Resilience4j**: Provides the `CircuitBreaker` implementation to handle fault tolerance during Kafka publishing.
*   **Jackson**: Utilized via `JsonUtil` for serializing event objects into JSON format.
*   **Java Management Extensions (JMX)**: Uses `ManagementFactory` and `OperatingSystemMXBean` to retrieve low-level system performance data.
*   **Common Library**: Depends on `com.cloudbalancer.common` for event definitions (`WorkerMetricsEvent`, `WorkerHeartbeatEvent`) and shared models.

## Usage Notes

*   **Fault Tolerance**: All outgoing Kafka messages are wrapped in a `CircuitBreaker`. If the circuit is open, the service will log a warning and skip the publication attempt, preventing the worker from hanging on network timeouts.
*   **Metrics Collection**: The `collectMetrics` method calculates CPU usage based on system load averages and memory usage via the JVM `Runtime` object. It also aggregates task-specific data (active, completed, failed tasks) from the `TaskExecutionService`.
*   **Configuration**: The intervals for metrics and heartbeats are externalized. Ensure these are set appropriately in the application configuration to balance monitoring granularity with network overhead.
*   **Serialization**: If an event fails to serialize (e.g., due to malformed data), the error is logged as an `ERROR` level event, but the service continues to function.
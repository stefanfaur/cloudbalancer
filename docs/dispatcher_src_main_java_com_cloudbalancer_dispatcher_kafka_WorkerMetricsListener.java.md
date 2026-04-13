# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/WorkerMetricsListener.java

## Overview

The `WorkerMetricsListener` is a Spring `@Component` that serves as the Kafka message consumer for the `dispatcher` service. Its primary responsibility is to ingest real-time performance metrics published by worker nodes. By listening to the `workers.metrics` Kafka topic, it deserializes incoming JSON payloads into `WorkerMetricsEvent` objects and delegates the processing of these metrics to the `AutoScalerService`.

## Public API

### `WorkerMetricsListener(AutoScalerService autoScalerService)`
Constructs a new `WorkerMetricsListener` with the required `AutoScalerService` dependency.

### `void onMetrics(String message)`
A Kafka listener method annotated with `@KafkaListener`. It consumes raw JSON strings from the `workers.metrics` topic, parses them, and invokes `autoScalerService.recordMetrics()` to update the system's internal state regarding worker CPU utilization.

## Dependencies

- `com.cloudbalancer.common.event.WorkerMetricsEvent`: Data model for incoming metrics.
- `com.cloudbalancer.common.util.JsonUtil`: Utility for JSON deserialization.
- `com.cloudbalancer.dispatcher.service.AutoScalerService`: Service responsible for business logic related to auto-scaling based on worker metrics.
- `org.springframework.kafka.annotation.KafkaListener`: Spring Kafka framework annotation for message consumption.
- `org.slf4j.Logger`: Logging framework for operational monitoring.

## Usage Notes

- **Kafka Topic**: This component listens specifically to the `workers.metrics` topic.
- **Consumer Group**: It operates within the `dispatcher-autoscaler` consumer group.
- **Error Handling**: The `onMetrics` method includes a `try-catch` block to handle deserialization errors or processing failures, ensuring that a malformed message does not crash the listener thread. Errors are logged at the `ERROR` level.
- **Integration**: This listener acts as the bridge between the Kafka message bus and the `AutoScalerService`. Ensure that the `AutoScalerService` is correctly configured in the Spring context to receive these updates.
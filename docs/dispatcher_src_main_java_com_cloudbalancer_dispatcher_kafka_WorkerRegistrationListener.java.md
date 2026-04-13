# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/WorkerRegistrationListener.java

## Overview

`WorkerRegistrationListener` is a Spring `@Component` that acts as a Kafka message consumer within the `dispatcher` service. Its primary responsibility is to listen for worker registration events published to the `workers.registration` Kafka topic. When a new worker node signals its availability, this listener processes the event and updates the internal `WorkerRegistryService` to track the new worker's state and capabilities.

## Public API

### `WorkerRegistrationListener(WorkerRegistryService workerRegistry)`
Constructs a new `WorkerRegistrationListener` with the required `WorkerRegistryService` dependency.

### `onWorkerRegistered(String message)`
A Kafka listener method annotated with `@KafkaListener`.
- **Topic**: `workers.registration`
- **Group ID**: `dispatcher`
- **Functionality**: Deserializes the incoming JSON message into a `WorkerRegisteredEvent`, extracts the worker's ID and capabilities, and registers the worker as `HEALTHY` in the `WorkerRegistryService`.

## Dependencies

- `com.cloudbalancer.common.event.WorkerRegisteredEvent`: Data model for the registration event.
- `com.cloudbalancer.common.model.WorkerHealthState`: Enum representing the health status of workers.
- `com.cloudbalancer.common.util.JsonUtil`: Utility for JSON deserialization.
- `com.cloudbalancer.dispatcher.service.WorkerRegistryService`: Service responsible for managing the registry of active worker nodes.
- `org.springframework.kafka.annotation.KafkaListener`: Spring Kafka framework annotation for message consumption.

## Usage Notes

- **Error Handling**: The `onWorkerRegistered` method includes a `try-catch` block to handle potential JSON deserialization errors or registry service failures. Errors are logged at the `ERROR` level, ensuring that a malformed message does not crash the listener thread.
- **Integration**: This component relies on the `dispatcher` consumer group. Ensure that the Kafka broker is configured to support this group and that the `workers.registration` topic is correctly provisioned in the environment.
- **Lifecycle**: As a Spring `@Component`, this class is automatically detected and instantiated by the Spring container during application startup, provided component scanning is enabled.
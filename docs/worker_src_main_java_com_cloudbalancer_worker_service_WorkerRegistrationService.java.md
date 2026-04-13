# File: worker/src/main/java/com/cloudbalancer/worker/service/WorkerRegistrationService.java

## Overview

The `WorkerRegistrationService` is a Spring-managed service responsible for the automatic registration of worker nodes within the CloudBalancer infrastructure. Upon application startup, this service broadcasts the worker's identity, hardware capabilities, and configuration tags to the cluster via Apache Kafka. This ensures that the dispatcher module is immediately aware of available resources for task scheduling.

## Public API

### `WorkerRegistrationService` (Constructor)
Initializes the service with the necessary Kafka infrastructure and worker configuration properties.

*   **Parameters**:
    *   `KafkaTemplate<String, String> kafkaTemplate`: Used to publish the registration event to the Kafka broker.
    *   `WorkerProperties properties`: Configuration object containing worker ID, resource limits, and capability definitions.

### `register()`
The primary lifecycle method, annotated with `@PostConstruct`. It is automatically invoked by the Spring container after dependency injection is complete.

*   **Functionality**:
    1.  Constructs a `WorkerCapabilities` object based on the provided `WorkerProperties`.
    2.  Generates a unique `WorkerRegisteredEvent` containing the worker's ID, current timestamp, and capabilities.
    3.  Serializes the event to JSON.
    4.  Publishes the event to the `workers.registration` Kafka topic.
*   **Throws**: `RuntimeException` if JSON serialization fails.

## Dependencies

*   **Spring Framework**: Uses `@Service` for component scanning and `@PostConstruct` for lifecycle management.
*   **Spring Kafka**: Utilizes `KafkaTemplate` for asynchronous event messaging.
*   **Jackson**: Used via `JsonUtil` for serializing registration events.
*   **CloudBalancer Common**: Relies on shared domain models (`WorkerRegisteredEvent`, `ResourceProfile`, `WorkerCapabilities`) and utility classes.

## Usage Notes

*   **Lifecycle**: This service is designed to run exactly once per worker instance startup. Ensure that the Kafka broker is reachable during the application context initialization phase, otherwise, the `register()` method may fail.
*   **Configuration**: The registration payload is heavily dependent on the `WorkerProperties` bean. Ensure that `cpuCores`, `memoryMb`, and `supportedExecutors` are correctly defined in the application configuration (e.g., `application.yml`) to ensure the dispatcher receives accurate resource data.
*   **Topic**: The service publishes to the `workers.registration` topic. Ensure that the dispatcher service is configured to consume from this specific topic to maintain a synchronized view of the worker pool.
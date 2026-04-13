# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/EventPublisher.java

## Overview

The `EventPublisher` is a Spring `@Component` responsible for reliably publishing messages and events to Apache Kafka topics within the `dispatcher` service. It acts as a wrapper around `KafkaTemplate`, providing integrated fault tolerance through the Resilience4j `CircuitBreaker` pattern. This ensures that if the Kafka producer experiences significant latency or failures, the system can gracefully handle the situation by dropping events rather than blocking the application threads.

## Public API

### `EventPublisher(KafkaTemplate<String, String> kafkaTemplate, CircuitBreaker circuitBreaker)`
Constructs a new `EventPublisher` instance.
*   **Parameters**:
    *   `kafkaTemplate`: The Spring Kafka template used for message transmission.
    *   `circuitBreaker`: A `@Qualifier("kafkaProducerCircuitBreaker")` injected Resilience4j circuit breaker instance to monitor producer health.

### `publishEvent(String topic, String key, CloudBalancerEvent event)`
Serializes a `CloudBalancerEvent` object to JSON and publishes it to the specified Kafka topic.
*   **Parameters**:
    *   `topic`: The target Kafka topic.
    *   `key`: The message key for partitioning.
    *   `event`: The `CloudBalancerEvent` instance to be serialized and sent.

### `publishMessage(String topic, String key, Object message)`
Serializes a generic object to JSON and publishes it to the specified Kafka topic.
*   **Parameters**:
    *   `topic`: The target Kafka topic.
    *   `key`: The message key for partitioning.
    *   `message`: The object to be serialized and sent.

## Dependencies

*   **Spring Kafka**: `org.springframework.kafka.core.KafkaTemplate` for message delivery.
*   **Resilience4j**: `io.github.resilience4j.circuitbreaker.CircuitBreaker` for fault tolerance.
*   **Jackson**: `com.fasterxml.jackson.core` for JSON serialization via `JsonUtil`.
*   **Common Library**: `com.cloudbalancer.common.event.CloudBalancerEvent` and `com.cloudbalancer.common.util.JsonUtil`.

## Usage Notes

*   **Circuit Breaker Integration**: Both publishing methods are wrapped in `circuitBreaker.executeRunnable()`. If the circuit breaker is in an `OPEN` state, the event/message will be dropped, and a warning will be logged. This prevents cascading failures when the Kafka cluster is unreachable.
*   **Serialization**: The class relies on `JsonUtil` for object-to-JSON conversion. If serialization fails (e.g., due to malformed objects), a `JsonProcessingException` is caught, and an error is logged; the message will not be sent.
*   **Error Handling**: The component is designed to be non-blocking for the caller in the event of Kafka producer issues, prioritizing system stability over guaranteed message delivery during outages.
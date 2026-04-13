# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/config/CircuitBreakerConfiguration.java

## Overview

The `CircuitBreakerConfiguration` class is a Spring `@Configuration` component that provides fault-tolerance infrastructure for the `dispatcher` service. It utilizes the Resilience4j library to manage circuit breaker instances, specifically configured to handle potential failures in Kafka producer operations.

## Public API

### `CircuitBreakerConfiguration`
The main configuration class that registers circuit breaker beans into the Spring application context.

### `circuitBreakerRegistry()`
*   **Return Type**: `CircuitBreakerRegistry`
*   **Description**: Creates and returns a default `CircuitBreakerRegistry` instance. This registry acts as a factory and manager for all circuit breaker instances within the application.

### `kafkaProducerCircuitBreaker(CircuitBreakerRegistry registry)`
*   **Return Type**: `CircuitBreaker`
*   **Parameters**: `CircuitBreakerRegistry registry`
*   **Description**: Defines a specialized `CircuitBreaker` bean named `kafkaProducerCircuitBreaker`. It is configured with a sliding window of 10 calls, a 50% failure rate threshold, a 30-second wait duration in the open state, and allows 3 calls in the half-open state.

## Dependencies

*   **io.github.resilience4j:resilience4j-circuitbreaker**: Core library used for implementing the circuit breaker pattern.
*   **org.springframework:spring-context**: Used for `@Configuration` and `@Bean` annotations to integrate with the Spring IoC container.
*   **java.time.Duration**: Used for defining time-based thresholds for the circuit breaker state transitions.

## Usage Notes

*   **Bean Injection**: To use the configured circuit breaker, inject the `kafkaProducerCircuitBreaker` bean into your service or component using the `@Qualifier("kafkaProducerCircuitBreaker")` annotation.
*   **Configuration Tuning**: The `kafkaProducerCircuitBreaker` is currently tuned for a sliding window of 10 requests. If the failure rate exceeds 50%, the circuit will trip to the `OPEN` state for 30 seconds before attempting to transition to `HALF_OPEN`. Adjust these parameters in the `kafkaProducerCircuitBreaker` method if the traffic volume or service reliability requirements change.
*   **Registry Access**: The `circuitBreakerRegistry` bean is available for global use if additional circuit breakers need to be created dynamically at runtime.
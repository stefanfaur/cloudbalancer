# File: worker/src/main/java/com/cloudbalancer/worker/config/CircuitBreakerConfiguration.java

## Overview

The `CircuitBreakerConfiguration` class is a Spring `@Configuration` component responsible for defining and managing fault-tolerance policies within the worker service. It leverages the [Resilience4j](https://resilience4j.guide/) library to implement the Circuit Breaker pattern, ensuring system stability by preventing cascading failures when interacting with external dependencies or result producers.

## Public API

### `CircuitBreakerConfiguration`

*   **`circuitBreakerRegistry()`**: Defines a Spring `@Bean` that provides the default `CircuitBreakerRegistry`. This registry acts as a factory and manager for all circuit breaker instances within the application context.
*   **`workerResultProducerCircuitBreaker(CircuitBreakerRegistry registry)`**: Defines a specific `@Bean` named `workerResultProducerCircuitBreaker`. This method configures a custom circuit breaker instance tailored for the `workerResultProducer` component with the following parameters:
    *   **Sliding Window Size**: 20 calls.
    *   **Failure Rate Threshold**: 70%.
    *   **Wait Duration in Open State**: 15 seconds.
    *   **Permitted Calls in Half-Open State**: 5 calls.

## Dependencies

*   **`io.github.resilience4j:resilience4j-circuitbreaker`**: Core library providing the circuit breaker implementation.
*   **`org.springframework:spring-context`**: Provides the `@Configuration` and `@Bean` annotations for Spring dependency injection.
*   **`java.time.Duration`**: Used for defining time-based thresholds in the circuit breaker configuration.

## Usage Notes

*   **Injection**: To use the configured circuit breaker in your services, inject the `CircuitBreaker` bean by name:
    ```java
    @Autowired
    @Qualifier("workerResultProducerCircuitBreaker")
    private CircuitBreaker workerResultProducerCircuitBreaker;
    ```
*   **Configuration Tuning**: The `workerResultProducer` circuit breaker is currently tuned for a 70% failure threshold over a window of 20 requests. If the service experiences high volatility, consider adjusting the `slidingWindowSize` or `failureRateThreshold` in the `workerResultProducerCircuitBreaker` method.
*   **Registry Usage**: If additional circuit breakers are required in the future, they should be registered via the `CircuitBreakerRegistry` bean provided by this configuration to ensure centralized management and monitoring.
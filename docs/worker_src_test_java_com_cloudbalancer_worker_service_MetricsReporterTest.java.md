# File: worker/src/test/java/com/cloudbalancer/worker/service/MetricsReporterTest.java

## Overview

`MetricsReporterTest` is a comprehensive JUnit 5 test suite designed to validate the `MetricsReporter` service within the `worker` module. This class ensures that the worker correctly captures system metrics, task execution statistics, and heartbeat signals, and subsequently publishes them to the appropriate Kafka topics.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a critical component of the worker's observability and health reporting infrastructure, any regressions here can lead to silent failures in cluster monitoring. Exercise extreme caution when modifying the underlying `MetricsReporter` logic or the Kafka event schemas.

## Public API

The test class does not expose a public API for production use, as it is strictly a test suite. However, it validates the following public methods of the `MetricsReporter` class:

- `publishMetrics()`: Triggers the collection of JVM and task-specific metrics and publishes them to the `workers.metrics` Kafka topic.
- `publishHeartbeat()`: Triggers the publication of a `WorkerHeartbeatEvent` to the `workers.heartbeat` Kafka topic to signal the worker's current health state.

## Dependencies

- **JUnit 5 / Mockito**: Used for test lifecycle management and mocking external dependencies.
- **KafkaTemplate**: Mocked to verify that events are correctly dispatched to the Kafka message bus.
- **TaskExecutionService**: Mocked to simulate various task states (active, completed, failed) and performance metrics.
- **CircuitBreaker**: Mocked to ensure that reporting logic is wrapped in resilience patterns.
- **WorkerProperties**: Used to verify configuration binding for reporting intervals.
- **JsonUtil**: Used to deserialize Kafka payloads back into POJOs for assertion verification.

## Usage Notes

### Testing Strategy
The tests utilize `ArgumentCaptor` to inspect the exact content of the messages sent to Kafka. This is critical because the `MetricsReporter` serializes objects to JSON strings before transmission.

### Key Test Scenarios
1. **Metrics Serialization**: Verifies that `WorkerMetricsEvent` contains valid JVM data (CPU, Heap, Thread count) and that the JSON structure matches the expected schema.
2. **Task Counter Accuracy**: Ensures that values retrieved from `TaskExecutionService` (e.g., `activeTaskCount`, `averageExecutionDurationMs`) are correctly mapped into the published metrics event.
3. **Heartbeat Integrity**: Confirms that the heartbeat event correctly identifies the worker and reports a `HEALTHY` state.
4. **Configuration Validation**: Verifies that `WorkerProperties` correctly handles custom intervals for metrics and heartbeats, ensuring the system respects user-defined reporting frequencies.

### Potential Pitfalls
- **Schema Mismatches**: Since the tests rely on `JsonUtil` to deserialize the captured Kafka payload, any breaking change in the `WorkerMetricsEvent` or `WorkerHeartbeatEvent` classes will cause these tests to fail. Always update the model classes and the test assertions in tandem.
- **Mocking the Circuit Breaker**: The `setUp` method includes a `lenient()` stub for the `CircuitBreaker`. If the `MetricsReporter` logic changes to require specific circuit breaker states (e.g., `OPEN` vs `CLOSED`), these tests must be updated to simulate those states explicitly rather than just executing the runnable.
- **Time-Sensitive Assertions**: While the current tests do not rely on real-time clocks, ensure that any future additions involving timestamps use a fixed clock or `Mockito` time-mocking to prevent flaky tests.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/kafka/WorkerMetricsListenerTest.java

## Overview

`WorkerMetricsListenerTest` is a JUnit 5 test suite designed to validate the behavior of the `WorkerMetricsListener` component. This listener is responsible for consuming Kafka messages containing worker performance metrics and delegating the processing of CPU utilization data to the `AutoScalerService`. The test suite ensures that the listener correctly parses incoming JSON payloads and handles malformed data without impacting system stability.

## Public API

### `WorkerMetricsListenerTest`
The test class utilizes `MockitoExtension` to mock dependencies and verify interactions between the listener and the auto-scaling infrastructure.

*   **`forwardsCpuMetricsToAutoScaler()`**: Verifies that a valid `WorkerMetricsEvent` JSON string is correctly deserialized and that the CPU usage percentage is successfully passed to the `AutoScalerService.recordMetrics` method.
*   **`handlesInvalidJsonGracefully()`**: Verifies that the listener does not throw exceptions or trigger service calls when provided with malformed or invalid JSON input, ensuring robust error handling.

## Dependencies

The test suite relies on the following components:

*   **JUnit 5 (Jupiter)**: Testing framework for test lifecycle management.
*   **Mockito**: Used for mocking the `AutoScalerService` and verifying method invocations.
*   **`com.cloudbalancer.common`**: Provides the data models (`WorkerMetrics`, `WorkerMetricsEvent`) and utility classes (`JsonUtil`) required for message serialization/deserialization.
*   **`com.cloudbalancer.dispatcher.service.AutoScalerService`**: The primary dependency being tested for interaction.

## Usage Notes

*   **Mocking Strategy**: The `AutoScalerService` is injected as a mock using `@Mock`. The `WorkerMetricsListener` is instantiated via `@InjectMocks`, allowing for isolated unit testing of the listener's logic.
*   **Test Data**: The `forwardsCpuMetricsToAutoScaler` test generates a `WorkerMetrics` object with realistic values (e.g., 85.5% CPU usage) to simulate actual Kafka message payloads.
*   **Error Handling**: The `handlesInvalidJsonGracefully` test confirms that the listener is resilient to input errors, which is critical for maintaining the stability of the Kafka consumer loop in production environments.
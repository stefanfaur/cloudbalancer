# File: common/src/test/java/com/cloudbalancer/common/event/WorkerMetricsEventTest.java

## Overview

`WorkerMetricsEventTest` is a JUnit 5 test suite responsible for validating the serialization and deserialization logic of the `WorkerMetricsEvent` class. It ensures that the event correctly integrates with the system's polymorphic event handling mechanism and maintains data integrity during JSON round-trips.

## Public API

### Methods

*   **`polymorphicSerializationRoundTrip()`**: Verifies that a `WorkerMetricsEvent` can be serialized to JSON and deserialized back into the base `CloudBalancerEvent` type while preserving all nested `WorkerMetrics` data and correctly identifying the event type.
*   **`jsonContainsEventTypeDiscriminator()`**: Validates that the serialized JSON output explicitly includes the `eventType` discriminator field, which is required for proper polymorphic deserialization by the Jackson `ObjectMapper`.

## Dependencies

*   **`com.cloudbalancer.common.model.WorkerMetrics`**: The data model representing worker performance metrics.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used for serialization.
*   **`com.fasterxml.jackson.databind.ObjectMapper`**: Used for JSON processing.
*   **`org.junit.jupiter.api.Test`**: JUnit 5 testing framework.
*   **`org.assertj.core.api.Assertions`**: Used for fluent assertion checks.

## Usage Notes

*   **Polymorphism**: This test confirms that `WorkerMetricsEvent` is correctly registered within the `CloudBalancerEvent` polymorphic hierarchy. Any changes to the `eventType` field in the source class must be reflected in these tests.
*   **JSON Structure**: The tests enforce that the `eventType` field is present in the serialized JSON. If the JSON structure changes (e.g., changing the field name or removing the discriminator), the polymorphic deserialization will fail.
*   **Data Integrity**: The `polymorphicSerializationRoundTrip` test serves as a regression suite for the `WorkerMetrics` model, ensuring that all numeric and temporal fields are correctly mapped during the serialization process.
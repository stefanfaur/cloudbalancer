# File: common/src/test/java/com/cloudbalancer/common/model/WorkerMetricsTest.java

## Overview

`WorkerMetricsTest` is a JUnit 5 test suite responsible for verifying the data integrity and JSON serialization/deserialization logic of the `WorkerMetrics` model. It ensures that the model correctly maps to and from JSON formats, which is critical for the communication of worker health and performance data across the system.

## Public API

### `WorkerMetricsTest`
The test class utilizes the `JsonUtil` mapper to perform validation on the `WorkerMetrics` POJO.

*   **`serializationRoundTrip()`**: Validates that a `WorkerMetrics` object can be serialized to a JSON string and subsequently deserialized back into an identical object, ensuring no data loss or corruption occurs during the process.
*   **`jsonContainsExpectedFieldNames()`**: Verifies that the serialized JSON output contains all required field keys, ensuring compatibility with external consumers (such as the `web-dashboard` or other microservices) that rely on specific JSON property names.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api`)**: Provides the testing framework and annotations.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertion syntax.
*   **Jackson Databind (`com.fasterxml.jackson.databind`)**: Used for JSON processing.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used for consistent serialization across the project.
*   **`java.time.Instant`**: Used for timestamp validation within the metrics model.

## Usage Notes

*   **Testing Strategy**: These tests serve as a contract test for the `WorkerMetrics` model. Any changes to the fields in the `WorkerMetrics` class (e.g., renaming fields or changing data types) will likely cause these tests to fail.
*   **JSON Schema**: The `jsonContainsExpectedFieldNames` test implicitly enforces the JSON schema for `WorkerMetrics`. If a new field is added to the model, this test should be updated to include the new field name to ensure it is correctly exposed in the serialized output.
*   **Time Sensitivity**: The `serializationRoundTrip` test uses `Instant.now()`. While generally safe, ensure that the `JsonUtil` mapper is configured to handle `java.time` objects correctly (typically via the `JavaTimeModule`).
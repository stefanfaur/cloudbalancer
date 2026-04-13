# File: common/src/test/java/com/cloudbalancer/common/model/TaskDescriptorTest.java

## Overview

`TaskDescriptorTest` is a JUnit 5 test suite located in the `common` module. Its primary purpose is to verify the serialization and deserialization (round-trip) integrity of task-related data models using the `JsonUtil` mapper. It ensures that `TaskDescriptor` and `ExecutionAttempt` objects can be correctly converted to and from JSON, maintaining data consistency for optional fields and complex nested objects.

## Public API

The class contains the following test methods:

*   **`fullDescriptorRoundTrip()`**: Validates that a `TaskDescriptor` populated with all fields (including `ResourceProfile`, `TaskConstraints`, and `ExecutionPolicy`) maintains its state after JSON serialization and deserialization.
*   **`descriptorWithNullOptionalFields()`**: Verifies that `TaskDescriptor` handles null values for optional fields (such as `constraints`, `executionPolicy`, and `taskIO`) correctly during the JSON round-trip process.
*   **`executionAttemptRoundTrip()`**: Confirms that an `ExecutionAttempt` object, including temporal data (`Instant`) and resource metrics, is correctly serialized and deserialized.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api.Test`)**: Provides the testing framework annotations.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertion syntax.
*   **Jackson (`com.fasterxml.jackson.databind.ObjectMapper`)**: Used for JSON processing.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used across the application.
*   **`java.time.Instant`**: Used for testing temporal fields in `ExecutionAttempt`.

## Usage Notes

*   **JSON Mapping**: These tests rely on the `JsonUtil.mapper()` configuration. Any changes to the global JSON serialization strategy (e.g., handling of nulls or date formats) should be verified against these tests to prevent regressions in data persistence.
*   **Round-Trip Testing**: The tests follow a standard "create -> serialize -> deserialize -> assert" pattern. When adding new fields to `TaskDescriptor` or `ExecutionAttempt`, update these tests to include the new fields to ensure they are correctly mapped.
*   **Optional Fields**: The `descriptorWithNullOptionalFields` test specifically ensures that the system does not crash or throw unexpected exceptions when optional components of a task are omitted.
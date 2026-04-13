# File: common/src/test/java/com/cloudbalancer/common/executor/LogMessageTest.java

## Overview

`LogMessageTest` is a unit test class designed to verify the integrity of the `LogMessage` data model during the serialization and deserialization process. It ensures that `LogMessage` objects can be correctly converted to and from JSON format using the system's `JsonUtil` configuration, maintaining all field values including `taskId`, log content, error status, and timestamps.

## Public API

### `LogMessageTest`
The test class contains the following test case:

*   **`serializationRoundTrip()`**: Performs a round-trip test by creating a `LogMessage` instance, serializing it to a JSON string, and deserializing it back into a `LogMessage` object. It asserts that the resulting object matches the original input values.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api.Test`)**: Used as the testing framework.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertion syntax.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the `ObjectMapper` instance required for JSON serialization/deserialization.
*   **`java.time.Instant`**: Used for timestamp verification.
*   **`java.util.UUID`**: Used for generating unique task identifiers.

## Usage Notes

*   This test serves as a contract verification for the `LogMessage` class to ensure it remains compatible with the system's JSON serialization requirements.
*   If new fields are added to `LogMessage`, this test should be updated to include assertions for those fields to prevent data loss during transmission.
*   The test relies on `JsonUtil.mapper()`, implying that `LogMessage` must be compatible with the default Jackson configuration (e.g., having appropriate constructors or annotations for deserialization).
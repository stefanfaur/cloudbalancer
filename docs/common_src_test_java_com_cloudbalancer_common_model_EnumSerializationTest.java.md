# File: common/src/test/java/com/cloudbalancer/common/model/EnumSerializationTest.java

## Overview

`EnumSerializationTest` is a comprehensive unit test suite designed to verify the JSON serialization and deserialization integrity of core enumeration types within the `com.cloudbalancer.common.model` package. By utilizing JUnit 5's `@ParameterizedTest` and `@EnumSource`, the suite ensures that every constant defined in the system's critical enums can be correctly converted to JSON and back to its original object form without data loss or mapping errors.

## Public API

The class provides a series of test methods that perform round-trip serialization validation for the following domain models:

*   **`priorityRoundTrip(Priority value)`**: Validates `Priority` enum serialization.
*   **`taskStateRoundTrip(TaskState value)`**: Validates `TaskState` enum serialization.
*   **`executorTypeRoundTrip(ExecutorType value)`**: Validates `ExecutorType` enum serialization.
*   **`workerHealthStateRoundTrip(WorkerHealthState value)`**: Validates `WorkerHealthState` enum serialization.
*   **`backoffStrategyRoundTrip(BackoffStrategy value)`**: Validates `BackoffStrategy` enum serialization.
*   **`failureActionRoundTrip(FailureAction value)`**: Validates `FailureAction` enum serialization.
*   **`securityLevelRoundTrip(SecurityLevel value)`**: Validates `SecurityLevel` enum serialization.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for the testing framework and parameterized test annotations.
*   **AssertJ**: Used for fluent assertion syntax (`assertThat`).
*   **Jackson Databind**: Used via `JsonUtil` to perform the actual JSON serialization/deserialization logic.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used across all tests to ensure consistency with production serialization settings.

## Usage Notes

*   **Automated Coverage**: These tests automatically cover all constants defined within the specified enums. If a new constant is added to any of these enums, the test suite will automatically include it in the next execution.
*   **Consistency**: The tests rely on `JsonUtil.mapper()`. If custom serialization logic (e.g., `@JsonValue` or `@JsonProperty`) is added to any of these enums, these tests act as the primary safeguard to ensure that the custom logic does not break round-trip compatibility.
*   **Execution**: These tests are intended to be run as part of the standard Maven/Gradle build lifecycle to catch breaking changes in model serialization early in the development process.
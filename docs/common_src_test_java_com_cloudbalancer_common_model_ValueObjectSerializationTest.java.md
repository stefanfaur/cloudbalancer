# File: common/src/test/java/com/cloudbalancer/common/model/ValueObjectSerializationTest.java

## Overview

`ValueObjectSerializationTest` is a JUnit 5 test suite designed to verify the JSON serialization and deserialization integrity of core domain models within the `com.cloudbalancer.common.model` package. It ensures that complex value objects can be converted to JSON and back to their original state without loss of data, and validates that default values are correctly applied.

## Public API

The class contains the following test methods:

*   **`resourceProfileRoundTrip`**: Validates that `ResourceProfile` objects maintain state through a full JSON serialization/deserialization cycle.
*   **`resourceProfileDefaultsForOptionalFields`**: Verifies that optional fields in `ResourceProfile` (such as GPU and network requirements) default to expected values.
*   **`executionPolicyRoundTrip`**: Ensures `ExecutionPolicy` objects are correctly serialized and deserialized.
*   **`executionPolicyDefaults`**: Confirms that the `ExecutionPolicy.defaults()` factory method returns the expected default configuration.
*   **`taskConstraintsRoundTrip`**: Tests the serialization of `TaskConstraints`, specifically verifying that sets of tags and worker blacklists are preserved.
*   **`taskConstraintsEmpty`**: Verifies that an unconstrained `TaskConstraints` object serializes correctly and maintains an empty state.
*   **`taskIoRoundTrip`**: Validates the serialization of `TaskIO` objects, including nested `InputArtifact` and `OutputArtifact` collections.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle and execution.
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **Jackson Databind**: Used for JSON processing via `ObjectMapper`.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the shared `ObjectMapper` configuration used across the tests.
*   **Domain Models**: `ResourceProfile`, `ExecutionPolicy`, `TaskConstraints`, and `TaskIO`.

## Usage Notes

*   **Serialization Strategy**: These tests rely on the `ObjectMapper` provided by `JsonUtil`. Any changes to the global JSON configuration in `JsonUtil` will be reflected in these tests.
*   **Round-Trip Testing**: The "round-trip" tests are the primary mechanism for ensuring that domain objects are compatible with the system's persistence and communication layers. When adding new fields to these models, ensure a corresponding test case is updated or added here to prevent breaking serialization.
*   **Default Values**: The tests for defaults (e.g., `executionPolicyDefaults`) serve as documentation for the expected behavior of the system when optional configuration is omitted.
# File: common/src/test/java/com/cloudbalancer/common/model/KafkaDtoSerializationTest.java

## Overview

`KafkaDtoSerializationTest` is a unit test suite located in the `common` module. It validates the JSON serialization and deserialization logic for core Data Transfer Objects (DTOs) used in Kafka-based communication within the CloudBalancer system. The tests ensure that complex objects, including those with polymorphic types, maintain data integrity during the round-trip conversion process using the project's standard `ObjectMapper` configuration.

## Public API

The class contains the following test methods:

*   **`taskAssignmentRoundTrip()`**: Verifies that a `TaskAssignment` object can be serialized to JSON and deserialized back into an identical object, ensuring all nested fields (like `TaskDescriptor` and `ResourceProfile`) are preserved.
*   **`taskResultRoundTrip()`**: Validates the serialization and deserialization of `TaskResult` objects, confirming that execution metadata such as exit codes, stdout, and worker identifiers remain consistent.
*   **`taskAssignedEventPolymorphicDeserialization()`**: Tests the polymorphic deserialization capabilities of the system. It ensures that a `TaskAssignedEvent` serialized as a generic `CloudBalancerEvent` is correctly reconstructed as the specific `TaskAssignedEvent` subtype.

## Dependencies

*   **JUnit 5**: Used for the test framework and assertions.
*   **Jackson Databind**: The core library used for JSON processing.
*   **AssertJ**: Provides fluent assertions for verifying test outcomes.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used across the application to ensure consistent serialization rules.
*   **Domain Models**: Depends on `TaskAssignment`, `TaskResult`, `TaskDescriptor`, and `CloudBalancerEvent` to perform validation.

## Usage Notes

*   **Serialization Consistency**: These tests rely on the `JsonUtil.mapper()` configuration. Any changes to the global Jackson configuration (e.g., adding mix-ins or custom serializers) should be verified against these tests to prevent breaking Kafka message consumption.
*   **Polymorphism**: The `taskAssignedEventPolymorphicDeserialization` test is critical for verifying that the `@JsonTypeInfo` and `@JsonSubTypes` annotations on the `CloudBalancerEvent` hierarchy are correctly configured. If new event types are added to the system, they must be registered in the polymorphic hierarchy and validated with similar tests.
*   **Environment**: This class is a pure unit test and does not require an external Kafka broker or network connectivity to execute.
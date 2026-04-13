# File: common/src/test/java/com/cloudbalancer/common/model/WorkerInfoTest.java

## Overview

`WorkerInfoTest` is a JUnit 5 test suite located in the `common` module. It is responsible for verifying the data integrity, serialization/deserialization logic, and business rules associated with worker-related domain models. The test suite ensures that `WorkerInfo`, `WorkerCapabilities`, `ExecutorCapabilities`, and `WorkerMetrics` objects correctly maintain their state when converted to and from JSON using the project's standard `JsonUtil` mapper.

## Public API

The class provides the following test methods:

*   **`workerInfoRoundTrip()`**: Validates that a complete `WorkerInfo` object, including nested capabilities and metrics, can be serialized to JSON and deserialized back into an identical object.
*   **`executorCapabilitiesRoundTrip()`**: Verifies the JSON serialization cycle for `ExecutorCapabilities`, ensuring resource profiles and security levels are preserved.
*   **`workerCapabilitiesSupportsExecutor()`**: Tests the business logic within `WorkerCapabilities` to ensure it correctly identifies supported and unsupported `ExecutorType` values.
*   **`workerMetricsRoundTrip()`**: Validates the serialization cycle for `WorkerMetrics`, ensuring performance data (CPU, memory, task counts) remains accurate after JSON processing.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api`)**: Used for test lifecycle management and assertions.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Provides fluent assertion APIs for validating test outcomes.
*   **Jackson (`com.fasterxml.jackson.databind`)**: Used for JSON serialization/deserialization.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Provides the configured `ObjectMapper` instance used across the tests.
*   **Domain Models**: Depends on `WorkerInfo`, `WorkerCapabilities`, `ExecutorCapabilities`, `WorkerMetrics`, `ResourceProfile`, and `ExecutorType` within the `com.cloudbalancer.common.model` package.

## Usage Notes

*   **Serialization Testing**: These tests serve as a regression suite for the JSON mapping configuration. If new fields are added to any of the `Worker` model classes, these tests must be updated to ensure the `ObjectMapper` can handle the new schema.
*   **Time Handling**: The tests utilize `java.time.Instant.now()` for metrics and info objects. Because these are serialized, ensure that the `JsonUtil` mapper is configured to handle ISO-8601 timestamps correctly.
*   **Integration**: These tests are critical for ensuring that the `common` model objects are compatible with the `dispatcher` and `web-dashboard` modules, which rely on these models for inter-service communication and UI rendering.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/ExecutorCapabilityFilterTest.java

## Overview

`ExecutorCapabilityFilterTest` is a JUnit 5 test suite that validates the functionality of the `ExecutorCapabilityFilter` class. This filter is responsible for ensuring that task scheduling only considers workers that possess the specific `ExecutorType` required by a given task. The tests verify that workers lacking the necessary capabilities are correctly excluded from the scheduling pool, while capable workers are retained.

## Public API

### `ExecutorCapabilityFilterTest`

The test class provides the following test cases:

*   **`removesWorkersWithoutRequiredExecutor()`**: Verifies that when a task requires a specific executor (e.g., `DOCKER`), any worker that does not support that executor is filtered out of the list of available candidates.
*   **`keepsAllCapableWorkers()`**: Verifies that when multiple workers possess the required executor capability, all of them are correctly identified and retained by the filter.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Used to define the capability requirements for tasks and workers.
*   `org.junit.jupiter.api.Test`: JUnit 5 testing framework.
*   `org.assertj.core.api.Assertions`: Used for fluent assertion checks on the filter results.
*   `com.cloudbalancer.dispatcher.scheduling.FilterTestHelper`: Provides utility methods to construct mock tasks and workers for testing scenarios.

## Usage Notes

*   These tests rely on `FilterTestHelper` to generate mock `Task` and `Worker` objects. Ensure that any changes to the underlying model structures are reflected in the helper to maintain test integrity.
*   The tests assume a standard filtering contract where the `filter` method accepts a `Task` and a `List<Worker>` and returns a filtered `List<Worker>`.
*   These tests are part of the `dispatcher` module and should be executed as part of the standard Maven/Gradle build lifecycle to ensure scheduling logic remains consistent.
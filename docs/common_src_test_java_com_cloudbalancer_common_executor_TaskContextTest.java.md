# File: common/src/test/java/com/cloudbalancer/common/executor/TaskContextTest.java

## Overview

`TaskContextTest` is a JUnit 5 test suite designed to verify the behavior and state management of the `TaskContext` class. It ensures that task identifiers, working directories, and optional logging callbacks are correctly initialized and handled during the lifecycle of a task execution context.

## Public API

### `TaskContextTest`

The test class provides the following test cases:

*   **`taskContextWithoutLogCallbackHasNullCallback`**: Validates that when a `TaskContext` is initialized using the two-argument constructor (ID and working directory), the `logCallback` property is correctly set to `null`.
*   **`taskContextWithLogCallbackInvokesCallback`**: Verifies that when a `LogCallback` is provided during initialization, the `TaskContext` correctly delegates log line processing to the provided callback implementation.
*   **`twoArgConstructorPreservesFields`**: Ensures that the `TaskContext` constructor correctly stores and preserves the `taskId` and `workingDirectory` fields provided during instantiation.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management and assertions.
*   **AssertJ**: Used for fluent assertion syntax (`assertThat`).
*   **Java Standard Library**: `java.nio.file.Path`, `java.time.Instant`, `java.util.UUID`, `java.util.List`.
*   **System Under Test (SUT)**: `com.cloudbalancer.common.executor.TaskContext`.

## Usage Notes

*   **Temporary Directories**: The tests utilize the JUnit 5 `@TempDir` extension to provide a clean, isolated `Path` for the `workingDirectory` parameter, ensuring tests do not leave side effects on the local file system.
*   **Callback Testing**: The `taskContextWithLogCallbackInvokesCallback` test uses a simple `ArrayList` to capture inputs passed to the `LogCallback`, allowing for verification of the delegation logic without requiring complex mocks.
*   **Execution**: These tests are intended to be run as part of the standard Maven/Gradle build lifecycle for the `common` module.
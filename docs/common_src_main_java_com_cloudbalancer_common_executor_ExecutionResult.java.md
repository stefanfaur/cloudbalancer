# File: common/src/main/java/com/cloudbalancer/common/executor/ExecutionResult.java

## Overview

The `ExecutionResult` class is a Java `record` that encapsulates the outcome of a command or task execution within the `com.cloudbalancer.common.executor` package. It provides an immutable structure to store the exit status, standard output, standard error, execution duration, and timeout status of an operation.

## Public API

### `ExecutionResult` (Record)
- `int exitCode()`: Returns the process exit code.
- `String stdout()`: Returns the captured standard output.
- `String stderr()`: Returns the captured standard error.
- `long durationMs()`: Returns the duration of the execution in milliseconds.
- `boolean timedOut()`: Returns true if the execution was terminated due to a timeout.

### `succeeded()`
- **Signature**: `public boolean succeeded()`
- **Description**: Determines if the execution was successful.
- **Logic**: Returns `true` if the `exitCode` is `0` and `timedOut` is `false`; otherwise, returns `false`.

## Dependencies

This class is a standalone Java `record` and does not depend on external libraries or other internal project modules.

## Usage Notes

- **Immutability**: As a Java `record`, all fields are immutable. Once an `ExecutionResult` is instantiated, its values cannot be modified.
- **Success Criteria**: The `succeeded()` method is the primary utility for checking the health of an execution. It is important to note that a non-zero exit code or a timeout will result in `succeeded()` returning `false`, even if the `stderr` is empty.
- **Integration**: This record is frequently used by executor services within the `cloudbalancer` system to pass execution metadata between components, such as the `dispatcher` module.
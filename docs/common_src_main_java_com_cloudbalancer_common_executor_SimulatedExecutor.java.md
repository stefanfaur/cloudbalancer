# File: common/src/main/java/com/cloudbalancer/common/executor/SimulatedExecutor.java

## Overview

`SimulatedExecutor` is a concrete implementation of the `TaskExecutor` interface designed for testing and development purposes within the CloudBalancer ecosystem. It allows developers to simulate task execution, including configurable durations and probabilistic failure rates, without requiring actual computational resources or external infrastructure.

This executor is categorized under `ExecutorType.SIMULATED` and operates within a `SANDBOXED` security level, making it ideal for validating scheduling logic and fault-tolerance mechanisms in a controlled environment.

## Public API

### `execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`
Simulates the execution of a task.
- **Parameters**: 
    - `spec`: Configuration map containing `durationMs` (execution time) and `failProbability` (0.0 to 1.0).
    - `allocation`: Resource allocation context.
    - `context`: Task metadata.
- **Returns**: `ExecutionResult` indicating success or failure based on the configured probability.

### `validate(Map<String, Object> spec)`
Validates the input specification for the simulated task.
- **Returns**: `ValidationResult.ok()` if `durationMs` is non-negative and `failProbability` is within [0.0, 1.0]; otherwise, returns an error.

### `estimateResources(Map<String, Object> spec)`
Provides a static resource estimate for the task.
- **Returns**: A `ResourceEstimate` object (defaulting to 1 CPU core, 256MB RAM, and the specified duration).

### `getCapabilities()`
Retrieves the operational capabilities of the executor.
- **Returns**: `ExecutorCapabilities` defining the sandbox environment and resource profile.

### `getExecutorType()`
- **Returns**: `ExecutorType.SIMULATED`.

### `cancel(ExecutionHandle handle)`
A no-op method intended for future implementation of task cancellation.

## Dependencies

- `com.cloudbalancer.common.model.ExecutorCapabilities`
- `com.cloudbalancer.common.model.ExecutorType`
- `com.cloudbalancer.common.model.ResourceProfile`
- `com.cloudbalancer.common.model.SecurityLevel`
- `java.util.Map`
- `java.util.concurrent.ThreadLocalRandom`

## Usage Notes

- **Configuration**: The `spec` map passed to `execute` and `validate` should contain:
    - `durationMs` (Integer): The time in milliseconds the task will "sleep" to simulate work.
    - `failProbability` (Double): A value between 0.0 and 1.0 representing the likelihood of the task returning a failure result.
- **Testing**: This class is primarily intended for integration tests and performance benchmarking of the dispatcher and scheduler components.
- **Limitations**: As a stub, the `cancel` method does not currently perform any action. It is designed to be extended in future phases to support active task termination.
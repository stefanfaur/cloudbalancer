# File: common/src/main/java/com/cloudbalancer/common/executor/TaskExecutor.java

## Overview

The `TaskExecutor` interface defines the contract for executing, validating, and managing tasks within the CloudBalancer infrastructure. It provides a standardized mechanism for different types of task executors to interact with the system, enabling dynamic resource estimation, capability reporting, and lifecycle management for distributed tasks.

## Public API

### Methods

*   **`execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`**
    Executes a task based on the provided specification, allocated resources, and execution context. Returns an `ExecutionResult` representing the outcome of the operation.

*   **`validate(Map<String, Object> spec)`**
    Validates the provided task specification. Returns a `ValidationResult` indicating whether the specification is well-formed and supported by this executor.

*   **`estimateResources(Map<String, Object> spec)`**
    Calculates the resource requirements (CPU, memory, etc.) needed to execute the task defined by the specification. Returns a `ResourceEstimate` object.

*   **`getCapabilities()`**
    Returns the `ExecutorCapabilities` supported by this specific implementation, detailing the features and constraints of the executor.

*   **`getExecutorType()`**
    Returns the `ExecutorType` identifying the category or implementation class of the executor.

*   **`cancel(ExecutionHandle handle)`**
    Attempts to cancel an ongoing task execution associated with the provided `ExecutionHandle`.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorCapabilities`
*   `com.cloudbalancer.common.model.ExecutorType`
*   `java.util.Map`

## Usage Notes

*   **Implementation**: Concrete implementations of `TaskExecutor` should be registered within the application context (e.g., via `ExecutorConfig`) to be discoverable by the worker nodes.
*   **Validation**: Always invoke `validate()` before attempting to `execute()` a task to ensure the specification is compatible with the executor's current configuration.
*   **Resource Management**: The `estimateResources()` method is critical for the scheduler to make informed decisions about where to place tasks; ensure this method provides accurate projections based on the input `spec`.
*   **Lifecycle**: The `cancel()` method should be implemented to gracefully terminate resources if a task is aborted by the dispatcher or the worker agent.
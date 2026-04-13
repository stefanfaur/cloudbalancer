# File: common/src/main/java/com/cloudbalancer/common/model/TaskResult.java

## Overview

The `TaskResult` class is an immutable Java `record` that encapsulates the final outcome of a task execution within the CloudBalancer system. It serves as a centralized data structure for tracking task performance, exit status, and metadata across distributed workers.

## Public API

### `TaskResult` (Record Components)
*   **taskId** (`UUID`): The unique identifier of the task.
*   **workerId** (`String`): The identifier of the worker node that executed the task.
*   **exitCode** (`int`): The process exit code returned by the task execution.
*   **stdout** (`String`): The standard output captured from the task.
*   **stderr** (`String`): The standard error captured from the task.
*   **executionDurationMs** (`long`): The total time taken for execution in milliseconds.
*   **timedOut** (`boolean`): Indicates whether the task exceeded its allocated time limit.
*   **completedAt** (`Instant`): The timestamp marking when the task finished.
*   **executionId** (`UUID`): The unique identifier for this specific execution attempt.

### Methods
*   **`succeeded()`**: Returns `true` if the task completed with an exit code of `0` and did not time out; otherwise, returns `false`.

## Dependencies

*   `java.time.Instant`: Used for precise timestamping of task completion.
*   `java.util.UUID`: Used for unique identification of tasks and execution instances.

## Usage Notes

*   **Immutability**: As a Java `record`, `TaskResult` is immutable. Once instantiated, the results of a task cannot be modified, ensuring thread safety and data integrity when passing results between the dispatcher and worker nodes.
*   **Success Logic**: The `succeeded()` method provides a convenient abstraction for checking task health. Developers should rely on this method rather than manually inspecting `exitCode` and `timedOut` fields to ensure consistent behavior across the application.
*   **Integration**: This record is frequently used in conjunction with bulk processing DTOs (such as `BulkResultEntry`) to aggregate multiple task outcomes for reporting or further processing.
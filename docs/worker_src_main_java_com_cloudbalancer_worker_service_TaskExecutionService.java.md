# File: worker/src/main/java/com/cloudbalancer/worker/service/TaskExecutionService.java

## Overview

The `TaskExecutionService` is the central orchestrator for task lifecycle management within the CloudBalancer worker node. It manages the end-to-end execution flow, including resource allocation, input artifact staging, process execution via registered `TaskExecutor` implementations, output artifact collection, and result reporting.

**Warning: Hotspot File**
This class is a high-activity component with high complexity. It is a frequent target for modifications and is critical to system stability. Changes here should be approached with caution, as errors can lead to task execution failures, resource leaks, or reporting inconsistencies.

## Public API

### Core Execution
*   **`void executeTask(TaskAssignment assignment)`**: The primary entry point for processing a task. It handles the full lifecycle: staging inputs, executing the task in a managed thread pool, enforcing timeouts, collecting outputs, and publishing results.

### Metrics & Monitoring
*   **`int getActiveTaskCount()`**: Returns the current number of tasks being processed.
*   **`long getCompletedTaskCount()`**: Returns the total number of successfully finished tasks.
*   **`long getFailedTaskCount()`**: Returns the total number of tasks that failed or timed out.
*   **`double getAverageExecutionDurationMs()`**: Calculates the mean duration of all completed tasks in milliseconds.

## Dependencies

*   **`KafkaTemplate`**: Used for publishing task results to the `tasks.results` topic.
*   **`CircuitBreaker`**: Protects the Kafka producer; if the circuit is open, result publishing is skipped to prevent system overload.
*   **`TaskExecutor` (List)**: A collection of strategy-based executors (e.g., Docker, Shell) used to run specific task types.
*   **`ArtifactService`**: Handles the movement of input/output files between the worker and remote storage.
*   **`WorkerChaosService`**: Injects latency or faults for testing system resilience.
*   **`ExecutorService`**: A cached thread pool used to isolate task execution from the main application thread.

## Usage Notes

### Execution Lifecycle
1.  **Chaos Injection**: The service first checks for injected latency via `WorkerChaosService`.
2.  **Staging**: A temporary directory is created, and `ArtifactService` fetches required inputs.
3.  **Execution**: The task is submitted to a cached thread pool. The service enforces a `timeoutSeconds` limit defined in the task policy.
4.  **Reporting**: Results are serialized to JSON and sent via Kafka. The `publishResult` method is wrapped in a `CircuitBreaker` to ensure that failures in the messaging layer do not crash the worker.
5.  **Cleanup**: Regardless of success or failure, the temporary working directory is recursively deleted to prevent disk exhaustion.

### Potential Pitfalls
*   **Resource Leaks**: If `cleanupWorkDir` fails (e.g., due to file locks), the worker node may eventually run out of disk space.
*   **Circuit Breaker State**: If the `workerResultProducerCircuitBreaker` opens, tasks will complete, but the dispatcher will not receive the results. Monitor the logs for `CallNotPermittedException`.
*   **Thread Pool Exhaustion**: The service uses `Executors.newCachedThreadPool()`. While flexible, a massive influx of tasks could lead to high memory consumption. Ensure that the dispatcher limits the rate of incoming `TaskAssignment` messages.
*   **Artifact Failures**: If `collectAndUploadArtifacts` fails, the task result is still published, but the output data may be lost. Always check logs for `Failed to collect/upload artifacts` warnings.

### Example: Handling a Task
When a `TaskAssignment` is received from Kafka:
1. The service identifies the correct `TaskExecutor` based on the `executorType` in the `TaskDescriptor`.
2. It creates a `TaskContext` which provides the task with a local `workDir` and a `RateLimitedLogCallback` to stream stdout/stderr back to the controller.
3. Upon completion (or timeout), the service flushes the logs and triggers the `ArtifactService` to upload results before cleaning up the local environment.
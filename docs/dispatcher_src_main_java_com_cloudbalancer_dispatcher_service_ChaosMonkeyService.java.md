# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/ChaosMonkeyService.java

## Overview

The `ChaosMonkeyService` is a critical resilience-testing component within the `dispatcher` module. It provides programmatic fault injection capabilities to simulate real-world infrastructure failures, such as worker crashes, task failures, and network latency. 

**Note:** This file is a **HOTSPOT** (top 25% for both change frequency and complexity). It is a high-risk area for bugs; changes to this service can directly impact the stability of the task scheduling and worker management lifecycle.

## Public API

The service exposes the following methods to trigger chaos scenarios:

*   **`killWorker(Optional<String> workerId)`**: Forces a worker into a `DEAD` state. If no ID is provided, it randomly selects a `HEALTHY` worker. It triggers the `WorkerFailureHandler` to initiate re-queuing of orphaned tasks.
*   **`failTask(Optional<UUID> taskId)`**: Injects a synthetic failure for a specific task by publishing a `TaskResult` with an exit code of `1` to the `tasks.results` Kafka topic. If no ID is provided, it targets a random `RUNNING` or `ASSIGNED` task.
*   **`injectLatency(String component, long delayMs, int durationSeconds)`**: Configures a temporary latency injection for a specific component.
*   **`checkAndApplyLatency(String component)`**: A hook intended to be called by other services to enforce injected latency. If an active, non-expired injection matches the component, the thread will sleep for the configured duration.

## Dependencies

*   **Persistence**: `WorkerRepository`, `TaskRepository` (for state lookups and updates).
*   **Messaging**: `KafkaTemplate` (for publishing synthetic task failures).
*   **Logic**: `WorkerFailureHandler` (to handle the side effects of killing a worker).
*   **Common**: `TaskResult`, `TaskState`, `WorkerHealthState`, `JsonUtil` (shared models and utilities).

## Usage Notes

### Hotspot Warning
As a high-activity file, ensure that any modifications to `killWorker` or `failTask` are accompanied by integration tests. Incorrect logic here can lead to "ghost" tasks or inconsistent worker states that are difficult to debug in production-like environments.

### Latency Injection
The latency injection mechanism uses an `AtomicReference` to store a single active `LatencyInjection` record. 
*   **Limitation**: Only one latency injection can be active at a time. Calling `injectLatency` will overwrite any existing active injection.
*   **Implementation**: The `checkAndApplyLatency` method performs a lazy cleanup of expired injections using `compareAndSet`.

### Example: Simulating a Worker Failure
To simulate a worker crash via the service:
```java
// Injecting a failure for a specific worker
chaosMonkeyService.killWorker(Optional.of("worker-123"));

// Or killing a random worker
chaosMonkeyService.killWorker(Optional.empty());
```

### Example: Simulating Task Failure
To force a task to report a failure:
```java
// Fail a specific task
chaosMonkeyService.failTask(Optional.of(myTaskId));

// Fail a random active task
chaosMonkeyService.failTask(Optional.empty());
```

### Integration with Kafka
The `failTask` method relies on the `tasks.results` Kafka topic. Ensure that the `KafkaTemplate` is correctly configured in the Spring context, as failures in serialization or publishing will throw a `RuntimeException`, potentially interrupting the calling thread.
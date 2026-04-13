# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/TestWorkerSimulator.java

## Overview

`TestWorkerSimulator` is a specialized integration testing utility that mimics the behavior of a production worker node in the CloudBalancer architecture. It facilitates end-to-end testing of the dispatcher by handling the full lifecycle of a worker: registering with the system, listening for task assignments via Kafka, executing tasks using configurable `TaskExecutor` implementations, and reporting results back to the dispatcher.

**Warning:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a critical component for integration testing, modifications here can cause cascading failures in the test suite. Ensure all changes are validated against existing integration test scenarios.

## Public API

### Constructors
*   `TestWorkerSimulator(String workerId, String bootstrapServers)`: Initializes a simulator with default `SIMULATED` executor capabilities.
*   `TestWorkerSimulator(String workerId, String bootstrapServers, Set<ExecutorType> supportedExecutorTypes, Map<ExecutorType, TaskExecutor> executors)`: Advanced constructor allowing injection of custom `TaskExecutor` implementations and specific capability sets.

### Methods
*   `void start()`: Registers the worker with the `workers.registration` topic and initializes a background thread to poll the `tasks.assigned` Kafka topic.
*   `void close()`: Gracefully shuts down the Kafka producer/consumer and the internal executor service. Implements `AutoCloseable`.

## Dependencies

*   **Kafka Clients**: Uses `org.apache.kafka` for event-driven communication.
*   **CloudBalancer Common**: Relies on `com.cloudbalancer.common.event`, `executor`, `model`, and `util` packages for domain objects (`TaskAssignment`, `TaskResult`, `WorkerCapabilities`, etc.).
*   **Jackson**: Used for JSON serialization/deserialization of events.
*   **Java Concurrency**: Uses `ExecutorService` and `Future` for managing task execution and background polling.

## Usage Notes

### Lifecycle Management
The simulator is designed to be used within a try-with-resources block or managed manually via `start()` and `close()`. 

### Integration Workflow
1.  **Registration**: Upon `start()`, the simulator sends a `WorkerRegisteredEvent` to the `workers.registration` topic.
2.  **Assignment**: The simulator polls `tasks.assigned`. It includes a retry loop to ensure the Kafka consumer has successfully joined a partition before proceeding.
3.  **Execution**: When a `TaskAssignment` is received:
    *   It looks up the appropriate `TaskExecutor` from the provided map.
    *   It executes the task within a dedicated `SingleThreadExecutor` with a configurable timeout.
    *   If the task exceeds the timeout, it generates a `TaskResult` with `timedOut = true`.
4.  **Reporting**: Results are published to the `tasks.results` topic.

### Example Usage
```java
// Define custom executors for specific task types
Map<ExecutorType, TaskExecutor> executors = Map.of(
    ExecutorType.DOCKER, new MyCustomDockerExecutor()
);

// Initialize and start
try (TestWorkerSimulator worker = new TestWorkerSimulator(
        "test-worker-01", 
        "localhost:9092", 
        Set.of(ExecutorType.DOCKER), 
        executors)) {
    
    worker.start();
    // Perform test assertions here...
} // Automatically closes and cleans up resources
```

### Potential Pitfalls
*   **Kafka Partitioning**: The consumer group ID is randomized (`workerId + UUID`) to ensure each simulator instance receives its own stream of assignments. If tests fail to receive assignments, verify that the `bootstrapServers` are reachable and the Kafka topic exists.
*   **Thread Safety**: The `running` volatile boolean is used to control the background polling loop. Ensure `close()` is called to prevent thread leaks during test teardown.
*   **Timeout Handling**: The simulator enforces a hard timeout based on the `executionPolicy`. If your integration tests involve long-running tasks, ensure the `timeoutSeconds` in the `TaskAssignment` descriptor is sufficient.
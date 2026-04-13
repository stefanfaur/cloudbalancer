# File: worker/src/test/java/com/cloudbalancer/worker/service/TaskExecutionServiceTest.java

## Overview

`TaskExecutionServiceTest` is a critical unit test suite for the `TaskExecutionService` component. It validates the core logic responsible for processing task assignments, managing execution lifecycles, and reporting results back to the system via Kafka.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As the primary verification layer for task execution logic, any regressions here indicate potential failures in worker task processing, resource management, or timeout handling.

## Public API

The class provides test coverage for the following scenarios:

*   **`setUp()`**: Initializes the test environment, specifically configuring the `CircuitBreaker` mock to execute provided runnables immediately, ensuring synchronous execution during tests.
*   **`executesTaskAndPublishesSuccessResult()`**: Verifies that a valid task assignment is processed, executed by the `SimulatedExecutor`, and results in a successful `TaskResult` published to the `tasks.results` Kafka topic.
*   **`executesTaskAndPublishesFailureResult()`**: Ensures that tasks configured to fail (e.g., via `failProbability`) are correctly identified, and the resulting `TaskResult` reflects a non-zero exit code and failure status.
*   **`respectsExecutionTimeout()`**: Validates the `ExecutionPolicy` enforcement. It ensures that tasks exceeding their defined timeout duration are terminated, marked as timed out, and reported as failures.

## Dependencies

*   **JUnit 5 & Mockito**: Used for test lifecycle management and mocking external services.
*   **KafkaTemplate**: Mocked to verify message publishing to the task result pipeline.
*   **CircuitBreaker**: Mocked to simulate resilience patterns during task execution.
*   **SimulatedExecutor**: Used as the concrete implementation for testing task execution logic without requiring actual infrastructure.
*   **Common Models**: Relies on `TaskDescriptor`, `TaskAssignment`, and `TaskResult` from the `com.cloudbalancer.common.model` package.

## Usage Notes

### Implementation Rationale
The test suite uses `ArgumentCaptor` to inspect the JSON payload sent to Kafka. This is essential because the `TaskExecutionService` serializes `TaskResult` objects into strings before transmission. By deserializing these strings back into `TaskResult` objects within the test, we ensure that the contract between the worker and the rest of the system remains intact.

### Potential Pitfalls
1.  **Mocking CircuitBreaker**: The `setUp` method uses `doAnswer` to force the `CircuitBreaker` to execute the runnable. If the production implementation of the `CircuitBreaker` changes its threading model or execution behavior, these tests may pass while production code fails.
2.  **Timing Sensitivity**: The `respectsExecutionTimeout` test relies on `System.currentTimeMillis()`. While currently stable, tests involving timeouts can become "flaky" if the CI environment is under heavy load.
3.  **JSON Serialization**: Since the service uses `JsonUtil` for serialization, ensure that any changes to the `TaskResult` model are reflected in the test assertions.

### Example: Adding a New Test Case
To test a new execution policy or executor type:

1.  **Define the Descriptor**: Create a `TaskDescriptor` with the specific `ExecutorType` and `ExecutionPolicy` required.
2.  **Prepare Assignment**: Instantiate a `TaskAssignment` with a unique `UUID`.
3.  **Execute**: Call `service.executeTask(assignment)`.
4.  **Verify**: Use `verify(kafkaTemplate).send(...)` to capture the output and assert against the expected `TaskResult` properties.

```java
// Example snippet for testing a custom policy
@Test
void testCustomPolicyBehavior() {
    // 1. Setup
    var descriptor = new TaskDescriptor(...); 
    var assignment = new TaskAssignment(UUID.randomUUID(), descriptor, ...);
    
    // 2. Act
    service.executeTask(assignment);
    
    // 3. Assert
    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
}
```
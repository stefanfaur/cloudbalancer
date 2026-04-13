# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/AutoScalingLifecycleTest.java

## Overview

`AutoScalingLifecycleTest` is a comprehensive integration test suite for the `cloudbalancer` dispatcher module. It validates the end-to-end lifecycle of worker nodes, including reactive scaling based on system metrics, manual scaling overrides, worker draining processes, and audit event publication.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. It serves as a critical verification point for the system's stability and is considered a high-risk area for regressions.

## Public API

The class provides the following test methods to verify the `AutoScalerService` and `WorkerRegistryService` integration:

*   **`reactiveScaleUpLifecycle()`**: Simulates high CPU load across registered workers and verifies that the `AutoScalerService` triggers a scale-up action and updates the repository.
*   **`manualScaleUpAddsWorker()`**: Validates that manual scaling requests bypass metric evaluation and successfully increase the worker count.
*   **`scaleDownDrainsWorker()`**: Verifies that when system conditions (low CPU, empty queue) are met, the system transitions workers to a `DRAINING` state.
*   **`respectsMaxWorkersBoundOnManualTrigger()`**: Ensures that manual scaling requests are rejected if they exceed the configured maximum worker capacity defined in the `ScalingPolicy`.
*   **`auditTrailPublishesScalingEvents()`**: Confirms that every scaling decision is serialized and published to the `system.scaling` Kafka topic for external auditing.
*   **`drainingWorkerExcludedFromScheduling()`**: Validates that workers marked as `DRAINING` are correctly removed from the pool of available workers for task scheduling.

## Dependencies

This test suite relies on the following core components:

*   **Spring Boot Test**: Provides the application context and environment for integration testing.
*   **Testcontainers (Kafka)**: Spins up a transient Kafka broker to verify event-driven audit trails.
*   **Awaitility**: Used for asynchronous assertions, allowing the test to poll for state changes (e.g., database updates or Kafka messages) within a defined timeout.
*   **WorkerRepository & WorkerRegistryService**: Used to manipulate and inspect the state of worker nodes in the persistence layer.
*   **AutoScalerService**: The primary system under test, responsible for scaling logic.

## Usage Notes

### Test Environment Setup
The test uses `@SpringBootTest` with `cloudbalancer.dispatcher.scaling.enabled=true`. The `setUp()` method ensures a clean state by:
1.  Clearing the `WorkerRepository`.
2.  Reloading the `ScalingPolicyService`.
3.  Resetting the `AutoScalerService` internal state.
4.  Initializing a `KafkaConsumer` to monitor the `system.scaling` topic.

### Handling Asynchronous Operations
Because scaling operations are often asynchronous, always use `await()` when asserting state changes in the database or Kafka. 

**Example: Verifying a Scale-Up**
```java
// 1. Trigger the scaling event
autoScaler.recordMetrics("w1", 95.0);
autoScaler.evaluate();

// 2. Use Awaitility to poll the repository for the expected change
await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
    assertThat(workerRepository.findAll().size()).isGreaterThan(initialCount);
});
```

### Common Pitfalls
*   **Kafka Consumer Lag**: Ensure the `KafkaConsumer` is properly closed in `@AfterEach` to prevent resource leaks or port conflicts in subsequent tests.
*   **Policy Conflicts**: If tests fail unexpectedly, check if `ScalingPolicy` settings (like `minWorkers` or `maxWorkers`) were modified by a previous test; always use `policyService.updatePolicy()` or `reloadPolicy()` to ensure a known state.
*   **Timing Sensitivity**: When testing `scaleDown` logic, ensure `advanceWindowForTest` is used to simulate the passage of time required by the scaling cooldown periods.
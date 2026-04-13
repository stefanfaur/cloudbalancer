# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/AutoScalerServiceTest.java

## Overview

`AutoScalerServiceTest` is a critical JUnit 5 test suite that validates the logic governing the dynamic scaling of worker nodes within the `cloudbalancer` dispatcher module. 

**Note: This file is a HOTSPOT.** It represents a high-risk area for bugs due to its frequent changes and central role in system stability. It exercises the `AutoScalerService` against various scenarios, including CPU-based scaling, queue-pressure-based scaling, cooldown enforcement, and boundary constraints (min/max worker counts).

The test suite uses `Mockito` to mock the infrastructure layer (`NodeRuntime`, `TaskRepository`, `EventPublisher`) and the service layer (`WorkerRegistryService`, `ScalingPolicyService`), allowing for deterministic testing of complex temporal and reactive scaling logic.

## Public API

The test class does not expose a public API as it is a test suite. However, it validates the following primary methods of `AutoScalerService`:

*   `recordMetrics(String workerId, double cpuLoad)`: Injects CPU metrics for a specific worker.
*   `evaluate()`: Triggers the core scaling logic based on collected metrics and current policy.
*   `scheduledEvaluate()`: Triggers periodic evaluation, including queue pressure analysis.
*   `triggerManual(ScalingAction action, int count)`: Executes manual scaling overrides.
*   `advanceWindowForTest(Duration duration)`: A test-only method used to simulate the passage of time for window-based calculations.

## Dependencies

The test relies on the following components:
*   **JUnit 5 & Mockito**: For test lifecycle management and dependency mocking.
*   **AssertJ**: For fluent assertion syntax.
*   **`com.cloudbalancer.common`**: Provides domain models like `ScalingEvent`, `WorkerHealthState`, and `NodeRuntime`.
*   **`com.cloudbalancer.dispatcher`**: The core system under test, including `ScalingProperties`, `TaskRepository`, and `WorkerRecord`.

## Usage Notes

### Testing Scaling Logic
The tests simulate real-world scenarios by manipulating time and metrics. When adding new test cases, observe the following patterns:

1.  **Time Manipulation**: Because scaling decisions are window-based, use `autoScaler.advanceWindowForTest(Duration)` before calling `evaluate()` to ensure metrics fall within the required evaluation window.
2.  **Mocking Policy**: Always mock `ScalingPolicyService.getCurrentPolicy()` to return a specific `ScalingPolicy` object if your test depends on custom min/max bounds or cooldown periods.
3.  **Queue Pressure**: To test queue-based scaling, use `recordTaskSubmitted()` and `recordTaskCompleted()` in conjunction with `scheduledEvaluate()`. The ratio is calculated based on the delta between these two counters.

### Common Pitfalls
*   **Cooldown Violations**: If a test fails to trigger a scale-up, ensure that the `evaluate()` method is not being called within the `ScalingProperties.cooldown` period defined in the policy.
*   **Metric Windowing**: If `evaluate()` returns early, verify that the metrics recorded via `recordMetrics()` are not older than the `reactiveWindowSeconds` defined in `ScalingProperties`.
*   **Stateful Mocks**: The `AutoScalerService` maintains internal state (e.g., last decision time, task counters). Ensure that `setUp()` correctly initializes a fresh `AutoScalerService` instance for every test method to prevent cross-test contamination.

### Example: Testing a Custom Scaling Scenario
To verify a new scaling trigger, follow this pattern:

```java
@Test
void testCustomScalingScenario() {
    // 1. Arrange: Setup policy and initial state
    when(policyService.getCurrentPolicy()).thenReturn(myCustomPolicy);
    
    // 2. Act: Record metrics and advance time
    autoScaler.recordMetrics("worker-1", 99.0);
    autoScaler.advanceWindowForTest(Duration.ofSeconds(10));
    autoScaler.evaluate();
    
    // 3. Assert: Verify the expected side effect
    verify(nodeRuntime, times(1)).startWorker(any());
}
```
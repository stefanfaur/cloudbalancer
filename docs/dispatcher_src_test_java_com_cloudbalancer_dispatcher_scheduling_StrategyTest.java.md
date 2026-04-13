# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/StrategyTest.java

## Overview

`StrategyTest` is a comprehensive JUnit 5 test suite located in the `com.cloudbalancer.dispatcher.scheduling` package. It validates the core scheduling algorithms used by the cloud balancer to distribute tasks across available worker nodes.

**Note:** This file is a **HOTSPOT** (top 25% for both change frequency and complexity). It is a high-risk area for bugs; any modifications to scheduling logic should be accompanied by rigorous verification of these tests.

## Public API

The test suite validates the following scheduling strategies and their factory implementation:

*   **`RoundRobinStrategy`**: Ensures cyclic distribution of tasks across workers.
*   **`WeightedRoundRobinStrategy`**: Validates that tasks are distributed proportionally based on worker capacity.
*   **`LeastConnectionsStrategy`**: Confirms that tasks are routed to the worker with the lowest current load.
*   **`ResourceFitStrategy`**: Verifies that the scheduler selects the worker with the best available resource alignment.
*   **`CustomStrategy`**: Tests the ability to inject user-defined weights for scoring metrics.
*   **`SchedulingStrategyFactory`**: Validates the instantiation logic for all supported strategies and error handling for unknown types.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api`)**: Used for test lifecycle management and assertions.
*   **AssertJ (`org.assertj.core.api`)**: Provides fluent assertions for verifying strategy outputs.
*   **`FilterTestHelper`**: A utility class providing mock data (workers, tasks) for consistent test scenarios.
*   **`com.cloudbalancer.dispatcher.scheduling`**: The package under test, including all concrete `Strategy` implementations and `WorkerScorer` interfaces.

## Usage Notes

### Testing Strategy Logic
The tests utilize a `Map<String, WorkerScorer>` to simulate the environment in which strategies operate. When adding new strategies or modifying existing ones, ensure the following:

1.  **Edge Case Handling**: Always test behavior when the candidate worker list is empty (e.g., `roundRobinReturnsEmptyForNoCandidates`).
2.  **Weight Validation**: For `CustomStrategy` or weighted implementations, verify that the `getWeights()` method returns the expected configuration.
3.  **Factory Registration**: If a new strategy is added, it must be registered in `SchedulingStrategyFactory` and a corresponding test case added to `factoryCreatesAllStrategies`.

### Example: Adding a New Strategy Test
To test a new custom strategy, follow the pattern established in `customStrategyUsesProvidedWeights`:

```java
@Test
void myNewStrategyTest() {
    var strategy = new MyNewStrategy(Map.of("metric", 50));
    var workers = List.of(workerRecord("w1"), workerRecord("w2"));
    
    var selected = strategy.select(anyTask(), workers, scorers);
    
    assertThat(selected).isPresent();
    // Verify specific selection logic
}
```

### Potential Pitfalls
*   **Hotspot Risk**: Because this is a high-change file, ensure that refactoring a strategy does not break the `strategyNamesAndWeightsCorrect` test, which acts as a contract for external API consumers.
*   **Resource Mocking**: When testing `LeastConnectionsStrategy` or `ResourceFitStrategy`, ensure the `workerWithCapacity` helper is configured with realistic resource values to avoid false negatives in the selection logic.
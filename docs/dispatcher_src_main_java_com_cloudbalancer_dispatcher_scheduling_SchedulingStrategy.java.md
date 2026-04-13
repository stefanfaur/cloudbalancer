# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/SchedulingStrategy.java

## Overview

The `SchedulingStrategy` interface defines the contract for load-balancing algorithms within the `cloudbalancer` dispatcher module. It provides a standardized mechanism for selecting an optimal `WorkerRecord` from a list of candidates based on a specific task and a collection of registered `WorkerScorer` implementations.

This interface is central to the system's extensibility, allowing for the implementation of various scheduling policies (e.g., Round Robin, Least Loaded, or custom heuristic-based strategies) that can be dynamically managed and injected via the `SchedulingStrategyFactory`.

## Public API

### Methods

#### `select`
```java
Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates, Map<String, WorkerScorer> scorers)
```
Evaluates the provided `candidates` against the given `task` using the provided `scorers` to determine the most suitable worker.
*   **Parameters**:
    *   `task`: The `TaskRecord` currently being scheduled.
    *   `candidates`: A list of available `WorkerRecord` instances.
    *   `scorers`: A map of available `WorkerScorer` components used for evaluation.
*   **Returns**: An `Optional` containing the selected `WorkerRecord`, or `Optional.empty()` if no suitable worker is found.

#### `getName`
```java
String getName()
```
Returns the unique identifier or human-readable name of the scheduling strategy.

#### `getWeights`
```java
Map<String, Integer> getWeights()
```
Returns the configuration weights associated with the strategy. These weights typically define the influence of different `WorkerScorer` metrics on the final selection decision.

## Dependencies

*   `com.cloudbalancer.dispatcher.persistence.TaskRecord`: Represents the task metadata required for scheduling decisions.
*   `com.cloudbalancer.dispatcher.persistence.WorkerRecord`: Represents the worker node metadata used for selection.
*   `com.cloudbalancer.dispatcher.scheduling.WorkerScorer`: Interface for components that calculate suitability scores for workers.
*   `java.util.List`, `java.util.Map`, `java.util.Optional`: Standard Java collections and utility types.

## Usage Notes

*   **Implementation**: When implementing a new strategy, ensure that the `select` method handles empty candidate lists gracefully by returning `Optional.empty()`.
*   **Scoring**: The `scorers` map allows strategies to dynamically access various metrics (e.g., CPU usage, memory availability, latency) to inform the selection logic.
*   **Configuration**: The `getWeights` method is intended to expose the internal configuration of the strategy, which may be persisted or modified via the `SchedulingConfigService`.
*   **Factory Pattern**: Strategies should be registered and instantiated through the `SchedulingStrategyFactory` to ensure consistent lifecycle management within the application.
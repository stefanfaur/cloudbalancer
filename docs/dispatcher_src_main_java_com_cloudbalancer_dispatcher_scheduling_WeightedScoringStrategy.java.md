# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/WeightedScoringStrategy.java

## Overview

`WeightedScoringStrategy` is an abstract base class that implements the `SchedulingStrategy` interface. It provides a flexible framework for selecting the most suitable `WorkerRecord` for a given `TaskRecord` by calculating a weighted average of multiple scoring metrics. By aggregating scores from various `WorkerScorer` implementations based on configurable weights, it allows for nuanced load-balancing decisions.

## Public API

### Constructors
*   **`WeightedScoringStrategy(String name, Map<String, Integer> weights)`**: Initializes the strategy with a unique name and a map defining the weight assigned to each `WorkerScorer` identifier.

### Methods
*   **`Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates, Map<String, WorkerScorer> scorers)`**: Evaluates all candidate workers using the registered scorers and returns the worker with the highest computed weighted score.
*   **`String getName()`**: Returns the identifier for this strategy.
*   **`Map<String, Integer> getWeights()`**: Returns the immutable map of weights currently assigned to the scorers.
*   **`double computeWeightedScore(TaskRecord task, WorkerRecord worker, Map<String, WorkerScorer> scorers)`**: Calculates the aggregate score for a worker by multiplying individual scorer results by their respective weights and normalizing by the total weight.

## Dependencies

*   **`com.cloudbalancer.dispatcher.persistence.TaskRecord`**: Represents the task to be scheduled.
*   **`com.cloudbalancer.dispatcher.persistence.WorkerRecord`**: Represents the worker node being evaluated.
*   **`com.cloudbalancer.dispatcher.scheduling.WorkerScorer`**: Interface used to compute individual metrics for workers.
*   **`java.util`**: Utilizes `Comparator`, `List`, `Map`, and `Optional` for data handling and selection logic.

## Usage Notes

*   **Extensibility**: As an abstract class, `WeightedScoringStrategy` is intended to be extended by concrete implementations that define specific weight configurations or additional selection logic.
*   **Scorer Mapping**: The `select` method requires a `Map<String, WorkerScorer>` where the keys must match the keys provided in the `weights` map during construction. If a key in the weights map is missing from the scorers map, that specific metric is ignored.
*   **Normalization**: The `computeWeightedScore` method automatically handles normalization by dividing the total weighted sum by the sum of all active weights. If the total weight is zero, the method returns a score of 0.
*   **Immutability**: The weights map is stored as an immutable copy (`Map.copyOf`), ensuring that the strategy configuration remains thread-safe and consistent after initialization.
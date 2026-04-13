# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/ResourceFitStrategy.java

## Overview

The `ResourceFitStrategy` class is a concrete implementation of the `WeightedScoringStrategy` within the `com.cloudbalancer.dispatcher.scheduling` package. It is designed to evaluate worker suitability based on a weighted combination of resource availability and current queue depth. By prioritizing resource availability (80%) over queue depth (20%), this strategy favors nodes that have the most capacity to handle new tasks, ensuring efficient load distribution across the cloud infrastructure.

## Public API

### `ResourceFitStrategy`

*   **Constructor**: `public ResourceFitStrategy()`
    *   Initializes the strategy with the identifier `"RESOURCE_FIT"`.
    *   Configures the internal scoring weights:
        *   `resourceAvailability`: 80
        *   `queueDepth`: 20

## Dependencies

*   `java.util.Map`: Used for defining the weight configuration map passed to the superclass constructor.
*   `com.cloudbalancer.dispatcher.scheduling.WeightedScoringStrategy`: The base class from which `ResourceFitStrategy` inherits its scoring logic and configuration management.

## Usage Notes

*   **Inheritance**: This class extends `WeightedScoringStrategy`. Ensure that the parent class is correctly configured to interpret the weight map provided during initialization.
*   **Weighting Logic**: The strategy is heavily biased toward resource availability. It is best suited for environments where hardware utilization metrics are reliable and prioritized over transient network or task-queue latency.
*   **Integration**: This strategy is intended to be used by the `WorkerScorer` system to rank available workers. When selecting a worker, the dispatcher will use these weights to calculate a final suitability score, where higher scores indicate a better fit for the incoming task.
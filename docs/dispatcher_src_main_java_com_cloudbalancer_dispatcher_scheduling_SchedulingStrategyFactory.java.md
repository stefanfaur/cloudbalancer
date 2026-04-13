# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/SchedulingStrategyFactory.java

## Overview

The `SchedulingStrategyFactory` is a utility class responsible for the instantiation of load-balancing strategies within the `dispatcher` module. It implements the Factory design pattern to decouple the creation logic of scheduling algorithms from the components that utilize them. By providing a centralized entry point, it allows the system to dynamically select and initialize different scheduling behaviors based on configuration strings.

## Public API

### `SchedulingStrategyFactory`

*   **`create(String name, Map<String, Integer> weights)`**: 
    *   **Description**: Creates and returns an instance of a class implementing the `SchedulingStrategy` interface based on the provided strategy name.
    *   **Parameters**:
        *   `name` (String): The identifier for the strategy (e.g., "ROUND_ROBIN", "LEAST_CONNECTIONS").
        *   `weights` (Map<String, Integer>): A map of weight configurations, primarily used for the "CUSTOM" strategy.
    *   **Returns**: An instance of `SchedulingStrategy`.
    *   **Throws**: `IllegalArgumentException` if the provided `name` does not match any supported strategy.

## Dependencies

*   `java.util.Map`: Used for passing configuration weights to custom strategy implementations.
*   `com.cloudbalancer.dispatcher.scheduling.SchedulingStrategy` (Implicit): The interface type returned by the factory.
*   Concrete Strategy Implementations:
    *   `RoundRobinStrategy`
    *   `WeightedRoundRobinStrategy`
    *   `LeastConnectionsStrategy`
    *   `ResourceFitStrategy`
    *   `CustomStrategy`

## Usage Notes

*   **Case Insensitivity**: The factory converts the input `name` to uppercase, meaning strategy names are case-insensitive (e.g., "round_robin" and "ROUND_ROBIN" are treated identically).
*   **Custom Strategy Handling**: When using the "CUSTOM" strategy, if the `weights` map is null, the factory defaults to an empty immutable map (`Map.of()`) to prevent `NullPointerException` in the `CustomStrategy` constructor.
*   **Extensibility**: To add a new scheduling algorithm, a new class implementing `SchedulingStrategy` must be created, and a corresponding case must be added to the `switch` expression within the `create` method.
*   **Instantiation**: The class has a private constructor to prevent instantiation, as it is designed to be used as a static utility provider.
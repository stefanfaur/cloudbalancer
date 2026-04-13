# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/StrategyRequest.java

## Overview

The `StrategyRequest` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It encapsulates the configuration parameters required to define or update a load-balancing strategy within the cloud balancer system.

## Public API

### Constructors
*   `StrategyRequest(String strategy, Map<String, Integer> weights)`: Constructs a new `StrategyRequest` with the specified strategy identifier and associated weight configuration.

### Fields
*   `String strategy`: The name or identifier of the load-balancing strategy to be applied.
*   `Map<String, Integer> weights`: A map where keys represent target identifiers (e.g., server nodes or clusters) and values represent their respective weightings for the chosen strategy.

### Methods
As a Java `record`, this class automatically provides:
*   `String strategy()`: Accessor for the strategy name.
*   `Map<String, Integer> weights()`: Accessor for the weight map.
*   `boolean equals(Object o)`: Standard equality check.
*   `int hashCode()`: Standard hash code implementation.
*   `String toString()`: String representation of the object.

## Dependencies

*   `java.util.Map`: Used to store the key-value pairs representing node weights.

## Usage Notes

*   **Immutability**: Being a `record`, instances of this class are immutable. Once created, the strategy name and weight map cannot be modified.
*   **Data Integrity**: Ensure that the `weights` map is non-null when instantiating this object to avoid potential `NullPointerException` issues during downstream processing.
*   **Context**: This DTO is typically used in requests sent to the dispatcher service to reconfigure how traffic is distributed across available infrastructure.
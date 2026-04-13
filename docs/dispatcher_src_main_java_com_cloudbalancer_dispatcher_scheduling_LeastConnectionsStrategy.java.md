# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/LeastConnectionsStrategy.java

## Overview

The `LeastConnectionsStrategy` class is a specialized implementation of the `WeightedScoringStrategy` used within the cloud balancer's task scheduling system. Its primary purpose is to prioritize worker nodes that currently maintain the lowest number of active connections, thereby promoting load distribution across the cluster.

This strategy is configured with a specific scoring profile that includes a default `queueDepth` weight of 100, which influences how the dispatcher evaluates worker availability.

## Public API

### `LeastConnectionsStrategy`

```java
public LeastConnectionsStrategy()
```

Constructs a new `LeastConnectionsStrategy` instance. It initializes the strategy with the identifier `"LEAST_CONNECTIONS"` and a default configuration map setting the `queueDepth` parameter to 100.

## Dependencies

*   `java.util.Map`: Used for defining the strategy's configuration parameters.
*   `com.cloudbalancer.dispatcher.scheduling.WeightedScoringStrategy`: The base class from which `LeastConnectionsStrategy` inherits its scoring logic and infrastructure.

## Usage Notes

*   **Inheritance**: This class extends `WeightedScoringStrategy`. Ensure that any changes to the scoring algorithm or configuration requirements are compatible with the base class implementation.
*   **Configuration**: The `queueDepth` parameter is currently hardcoded to 100. If dynamic tuning of this value is required in future iterations, consider refactoring the constructor to accept configuration arguments.
*   **Integration**: This strategy is intended to be used by the dispatcher's scheduling engine to rank `WorkerRecord` objects. It should be instantiated and registered within the application's dependency injection container or strategy registry.
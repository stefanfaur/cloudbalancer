# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/CustomStrategy.java

## Overview

The `CustomStrategy` class is a concrete implementation of the `WeightedScoringStrategy` within the `cloudbalancer` dispatcher module. It provides a mechanism for defining user-specified load-balancing behavior by allowing the injection of custom weight mappings. This strategy is typically utilized when standard load-balancing algorithms (such as Least Connections) are insufficient for specific infrastructure requirements.

## Public API

### `CustomStrategy`

```java
public CustomStrategy(Map<String, Integer> weights)
```

*   **Description**: Constructs a new `CustomStrategy` instance.
*   **Parameters**:
    *   `weights` (`Map<String, Integer>`): A map where keys represent resource identifiers or node attributes and values represent their corresponding priority or capacity weights.
*   **Behavior**: Initializes the strategy with the identifier `"CUSTOM"` and applies the provided weight configuration to the underlying `WeightedScoringStrategy` base class.

## Dependencies

*   `java.util.Map`: Used for defining the weight configuration schema.
*   `com.cloudbalancer.dispatcher.scheduling.WeightedScoringStrategy`: The base class that provides the core scoring logic and infrastructure for weight-based dispatching.

## Usage Notes

*   **Integration**: This class is intended to be instantiated via the `SchedulingStrategyFactory` when a custom load-balancing configuration is required.
*   **Configuration**: Ensure that the `Map<String, Integer>` passed to the constructor contains valid keys that correspond to the nodes or services managed by the dispatcher. Invalid or missing keys may result in default scoring behavior defined by the parent `WeightedScoringStrategy`.
*   **Extensibility**: As a subclass of `WeightedScoringStrategy`, it inherits the ability to perform weighted calculations for task distribution. If complex dynamic weighting is required, consider extending this class or implementing a custom logic provider that updates the weight map at runtime.
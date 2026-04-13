# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/StrategyResponse.java

## Overview

The `StrategyResponse` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a structured container for communicating the results of a load-balancing strategy selection process. It encapsulates the name of the chosen strategy and a corresponding map of weight distributions assigned to specific cloud resources or nodes.

## Public API

### `StrategyResponse`

```java
public record StrategyResponse(String strategy, Map<String, Integer> weights) {}
```

#### Components
*   **`strategy`** (`String`): The identifier or name of the load-balancing strategy currently in effect or selected.
*   **`weights`** (`Map<String, Integer>`): A key-value mapping where the key represents the resource identifier (e.g., node ID or server name) and the value represents the assigned weight or capacity share for that resource.

## Dependencies

*   `java.util.Map`: Used to store the association between resource identifiers and their respective integer weights.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable by design. Once an instance is created, the `strategy` name and the `weights` map cannot be modified.
*   **Data Serialization**: This DTO is intended for use in API responses. Ensure that the JSON serialization framework (e.g., Jackson) is configured to handle the `Map` structure correctly when converting this record to a response body.
*   **Null Safety**: While the record does not explicitly enforce non-null constraints, it is recommended to ensure that the `weights` map is initialized (even if empty) to avoid `NullPointerException` during downstream processing.
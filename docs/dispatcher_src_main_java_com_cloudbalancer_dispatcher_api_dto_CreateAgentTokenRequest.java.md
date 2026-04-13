# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/CreateAgentTokenRequest.java

## Overview

The `CreateAgentTokenRequest` is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a lightweight container for capturing the necessary information required to initiate the creation of a new agent token within the CloudBalancer system.

## Public API

### `CreateAgentTokenRequest`

```java
public record CreateAgentTokenRequest(String label) {}
```

#### Constructor
*   **`CreateAgentTokenRequest(String label)`**: Initializes a new request object with the specified label.

#### Fields
*   **`label`**: A `String` representing the human-readable identifier or name assigned to the agent token being created.

## Dependencies

This class has no external dependencies and relies solely on standard Java SE features.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the `label` cannot be modified.
*   **Serialization**: This DTO is intended to be used in conjunction with JSON serialization frameworks (such as Jackson) to map incoming API request bodies to Java objects.
*   **Implementation**: Ensure that the `label` provided is unique or follows the naming conventions required by the downstream service responsible for token generation to avoid conflicts or validation errors.
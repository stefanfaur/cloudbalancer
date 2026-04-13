# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AgentRegistrationRequest.java

## Overview

The `AgentRegistrationRequest` is a Java `record` used to encapsulate the data required for an agent to register itself with the CloudBalancer system. It serves as a Data Transfer Object (DTO) that carries essential hardware specifications and authentication credentials from the agent to the dispatcher.

## Public API

### `AgentRegistrationRequest`

```java
public record AgentRegistrationRequest(
    String agentId, 
    String token, 
    int cpuCores, 
    int memoryMb
) {}
```

#### Components
*   **`agentId` (String)**: A unique identifier assigned to the agent instance.
*   **`token` (String)**: An authentication token used to verify the agent's identity during the registration handshake.
*   **`cpuCores` (int)**: The number of CPU cores available on the agent machine.
*   **`memoryMb` (int)**: The total amount of RAM available on the agent machine, measured in megabytes.

## Dependencies

This class is a standard Java `record` and does not have any external library dependencies. It relies solely on the Java SE runtime environment (Java 14+).

## Usage Notes

*   **Immutability**: As a `record`, this class is immutable. Once an instance is created, its fields cannot be modified.
*   **Serialization**: This DTO is intended to be serialized/deserialized (e.g., via Jackson or Gson) when transmitted over the network between the agent and the dispatcher. Ensure that the JSON keys in the incoming request match the field names defined in the record.
*   **Validation**: This class does not contain built-in validation logic. It is recommended to apply JSR-303/JSR-380 (Bean Validation) annotations or manual validation logic in the service layer to ensure `cpuCores` and `memoryMb` contain positive, non-zero values.
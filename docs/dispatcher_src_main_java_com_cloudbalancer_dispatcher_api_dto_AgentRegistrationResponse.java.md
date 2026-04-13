# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AgentRegistrationResponse.java

## Overview

The `AgentRegistrationResponse` is a Java `record` used to encapsulate the configuration details required for an agent to connect to the internal message broker infrastructure. It serves as a Data Transfer Object (DTO) returned to agents upon successful registration with the Cloud Balancer dispatcher.

## Public API

### `AgentRegistrationResponse`

```java
public record AgentRegistrationResponse(
    String kafkaBootstrap, 
    String kafkaUsername, 
    String kafkaPassword
) {}
```

#### Components
*   **`kafkaBootstrap`**: The connection string (host:port) for the Kafka cluster.
*   **`kafkaUsername`**: The SASL/PLAIN username used for authenticating the agent with the Kafka broker.
*   **`kafkaPassword`**: The SASL/PLAIN password used for authenticating the agent with the Kafka broker.

## Dependencies

This class has no external dependencies and relies solely on standard Java 16+ `record` functionality.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, the connection credentials cannot be modified.
*   **Serialization**: This DTO is intended to be serialized to JSON when returned via the dispatcher's REST API. Ensure that the consuming agent's HTTP client is configured to deserialize these fields correctly.
*   **Security**: Since this object contains sensitive credentials (`kafkaPassword`), ensure that all transmissions of this DTO are performed over encrypted channels (HTTPS/TLS) and that the object is handled securely in memory.
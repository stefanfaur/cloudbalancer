# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/registration/RegistrationProperties.java

## Overview

`RegistrationProperties` is a Spring `@ConfigurationProperties` class that manages configuration settings for the registration module within the `dispatcher` service. It is bound to the `cloudbalancer.registration` configuration prefix, allowing external configuration via `application.properties` or `application.yml` files.

This class centralizes the Kafka connection parameters required for agent registration, providing default values to ensure the service can initialize even if specific environment variables are omitted.

## Public API

### `RegistrationProperties`
The main configuration bean.

*   **`getKafkaBootstrapExternal()` / `setKafkaBootstrapExternal(String)`**: Accessor/Mutator for the Kafka bootstrap server address. Defaults to `localhost:9092`.
*   **`getKafkaUsername()` / `setKafkaUsername(String)`**: Accessor/Mutator for the Kafka authentication username. Defaults to `cloudbalancer-agent`.
*   **`getKafkaPassword()` / `setKafkaPassword(String)`**: Accessor/Mutator for the Kafka authentication password. Defaults to `changeme`.

## Dependencies

*   `org.springframework.boot.context.properties.ConfigurationProperties`: Used to map external configuration properties to the class fields.
*   `org.springframework.stereotype.Component`: Marks the class as a Spring-managed bean, allowing it to be injected into other services.

## Usage Notes

*   **Configuration Prefix**: To override the default values, define properties in your Spring configuration file using the `cloudbalancer.registration` prefix:
    ```yaml
    cloudbalancer:
      registration:
        kafka-bootstrap-external: "kafka-broker:9092"
        kafka-username: "my-secure-user"
        kafka-password: "my-secure-password"
    ```
*   **Integration**: This class is primarily consumed by `AgentRegistrationController` to facilitate the registration process. When an agent requests registration, the controller retrieves these properties to provide the necessary Kafka connection details to the client.
*   **Security**: The default password `changeme` is intended for development purposes only. Ensure this is overridden in production environments via environment variables or secure configuration management.
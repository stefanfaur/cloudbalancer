# File: worker-agent/src/main/java/com/cloudbalancer/agent/config/KafkaRegistrationConfig.java

## Overview

`KafkaRegistrationConfig` is a Spring `@Configuration` class responsible for dynamically configuring Kafka connectivity for the worker agent. It is conditionally activated only when a valid `cloudbalancer.agent.registration-token` is present in the application environment.

This configuration handles the setup of Kafka producers and consumers, including the injection of security credentials (SASL/PLAIN) retrieved during the agent's registration process.

## Public API

### Beans
*   **`registrationResult(AgentRegistrationClient client)`**: Executes the agent registration process and returns the `RegistrationResult` containing Kafka connection details. Throws an `IllegalStateException` if registration fails.
*   **`producerFactory(RegistrationResult result)`**: Creates a `ProducerFactory` configured with SASL/PLAIN authentication using the registration result.
*   **`consumerFactory(RegistrationResult result, AgentProperties agentProps)`**: Creates a `ConsumerFactory` configured with SASL/PLAIN authentication and a unique group ID based on the agent's ID.
*   **`kafkaTemplate(ProducerFactory<String, String> producerFactory)`**: Provides a `KafkaTemplate` for sending messages.
*   **`kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory)`**: Provides a `ConcurrentKafkaListenerContainerFactory` for processing incoming Kafka messages.

### Conditionals
*   **`RegistrationTokenPresent`**: A static inner class implementing `Condition` that checks for the existence and validity of the `cloudbalancer.agent.registration-token` property.

## Dependencies

*   **Spring Kafka**: Used for `KafkaTemplate`, `ProducerFactory`, `ConsumerFactory`, and `ConcurrentKafkaListenerContainerFactory`.
*   **Apache Kafka Clients**: Used for configuration constants (`ProducerConfig`, `ConsumerConfig`, `SaslConfigs`).
*   **`AgentRegistrationClient`**: Used to perform the initial handshake and retrieve Kafka connection metadata.
*   **`AgentProperties`**: Used to identify the agent for consumer group configuration.

## Usage Notes

*   **Conditional Activation**: This configuration is ignored if the `cloudbalancer.agent.registration-token` property is missing or blank. Ensure this property is set in your `application.properties` or environment variables to enable Kafka communication.
*   **Security**: The configuration enforces `SASL_PLAINTEXT` with `PLAIN` mechanism. Credentials are automatically extracted from the `RegistrationResult` and injected into the Kafka client properties via the `addSaslProperties` helper method.
*   **Consumer Groups**: The consumer group ID is automatically generated using the pattern `agent-{agentId}`, ensuring that each agent instance maintains its own offset tracking.
*   **Error Handling**: If the registration client returns `null` despite the token being present, the application will fail to start, as the `registrationResult` bean will throw an `IllegalStateException`.
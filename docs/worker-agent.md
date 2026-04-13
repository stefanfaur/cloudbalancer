# Module: worker-agent

## Overview

The `worker-agent` module is a core component of the CloudBalancer ecosystem, designed to operate on worker nodes. It acts as a bridge between the central control plane and the local infrastructure, primarily managing the lifecycle of Docker-based worker containers. 

The agent is built as a Spring Boot application that leverages Apache Kafka for asynchronous communication, enabling it to receive control commands (start/stop workers) and report status updates (heartbeats and registration) back to the dispatcher.

## Public API Summary

The module exposes several key services and configuration components:

### Configuration
- **`AgentProperties`**: Binds external configuration (YAML/properties) to the application, managing settings for Docker, Kafka, and resource capacity.
- **`AgentConfig` / `DockerClientConfig` / `KafkaRegistrationConfig`**: Spring `@Configuration` classes that manage the lifecycle of essential beans, including the `DockerClient`, `ObjectMapper`, and Kafka infrastructure.

### Core Services
- **`ContainerManager`**: The primary service for Docker orchestration. It handles the creation, startup, and graceful shutdown of worker containers, ensuring the local state remains synchronized with the control plane.
- **`AgentRegistrationClient`**: Manages the initial handshake between the agent and the central dispatcher, validating registration tokens.

### Messaging
- **`AgentCommandListener`**: A Kafka listener that processes incoming control commands, translating them into actions for the `ContainerManager`.
- **`AgentHeartbeatPublisher`**: Responsible for periodically broadcasting the agent's health, resource availability, and registration status to the control plane.

## Architecture Notes

- **Event-Driven Control**: The agent follows an event-driven architecture using Apache Kafka. This decouples the agent from the dispatcher, allowing for resilient communication even if the control plane is temporarily unreachable.
- **Docker Orchestration**: The `ContainerManager` abstracts the complexities of the Docker API, providing a clean interface for the agent to manage container lifecycles. It includes startup reconciliation to ensure existing containers are tracked correctly.
- **Conditional Configuration**: The module uses Spring's conditional configuration (e.g., `KafkaRegistrationConfig`) to ensure that infrastructure components are only initialized when necessary, such as when a valid registration token is present.
- **Testing Strategy**: The module maintains high test coverage with dedicated JUnit 5 suites for each core component, ensuring that command processing, container lifecycle management, and network communication are verified in isolation.
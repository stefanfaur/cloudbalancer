# File: worker-agent/src/main/java/com/cloudbalancer/agent/kafka/AgentHeartbeatPublisher.java

## Overview

`AgentHeartbeatPublisher` is a core component of the `worker-agent` responsible for maintaining communication between the agent and the central control plane via Apache Kafka. It handles the lifecycle of the agent by publishing registration events upon startup and periodically broadcasting heartbeat signals to report current resource utilization and active container status.

## Public API

### `AgentHeartbeatPublisher(KafkaTemplate<String, String> kafkaTemplate, AgentProperties props, ContainerManager containerManager)`
Constructs a new publisher instance.
- **`kafkaTemplate`**: The Spring Kafka template used for message production.
- **`props`**: Configuration properties containing agent identity and hardware specifications.
- **`containerManager`**: Service used to query the current state of active worker containers.

### `publishRegistration()`
Annotated with `@PostConstruct`, this method is triggered automatically after bean initialization. It constructs an `AgentRegisteredEvent` containing the agent's hardware profile and publishes it to the `agents.registration` Kafka topic.

### `publishHeartbeat()`
Annotated with `@Scheduled`, this method executes periodically based on the `cloudbalancer.agent.heartbeat-interval-ms` configuration (defaulting to 10,000ms). It gathers resource usage metrics from the `ContainerManager`, constructs an `AgentHeartbeat` object, and publishes it to the `agents.heartbeat` Kafka topic.

## Dependencies

- **`com.cloudbalancer.agent.config.AgentProperties`**: Provides static configuration and hardware metadata.
- **`com.cloudbalancer.agent.service.ContainerManager`**: Provides real-time data regarding active worker containers.
- **`com.cloudbalancer.common.agent.*`**: Contains the DTOs (`AgentHeartbeat`, `AgentRegisteredEvent`) for Kafka messaging.
- **`org.springframework.kafka.core.KafkaTemplate`**: Infrastructure for Kafka message transmission.
- **`com.cloudbalancer.common.util.JsonUtil`**: Utility for serializing event objects into JSON format.

## Usage Notes

- **Kafka Topics**: The class expects the Kafka cluster to have `agents.registration` and `agents.heartbeat` topics available.
- **Scheduling**: The heartbeat interval is configurable via the `cloudbalancer.agent.heartbeat-interval-ms` property. If not specified, it defaults to 10 seconds.
- **Error Handling**: Both registration and heartbeat publishing are wrapped in `try-catch` blocks to prevent scheduling failures from crashing the agent process; errors are logged via SLF4J.
- **Resource Calculation**: The heartbeat logic currently assumes a static overhead of 2048MB of memory per active worker container. Ensure this aligns with the actual resource footprint of the containers managed by the `ContainerManager`.
# File: common/src/main/java/com/cloudbalancer/common/agent/AgentRegisteredEvent.java

## Overview

The `AgentRegisteredEvent` class is a Java `record` used to represent the registration of a new agent within the CloudBalancer infrastructure. It serves as a data transfer object (DTO) that captures the static capabilities and identification details of an agent at the moment it joins the cluster.

## Public API

### `AgentRegisteredEvent`

A immutable record containing the following components:

*   **`agentId`** (`String`): A unique identifier for the agent instance.
*   **`hostname`** (`String`): The network hostname of the machine where the agent is running.
*   **`totalCpuCores`** (`double`): The total number of CPU cores available to the agent.
*   **`totalMemoryMB`** (`long`): The total physical memory available to the agent in megabytes.
*   **`supportedExecutors`** (`Set<ExecutorType>`): A set of `ExecutorType` enums defining the types of tasks or workloads the agent is capable of processing.
*   **`timestamp`** (`Instant`): The precise time at which the registration event occurred.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Used to define the capabilities of the agent.
*   `java.time.Instant`: Used for precise event timestamping.
*   `java.util.Set`: Used to manage the collection of supported executor types.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified.
*   **Event-Driven Architecture**: This class is intended to be used as a payload in event-driven communication (e.g., via message queues or event buses) to notify the control plane that a new agent is available for task scheduling.
*   **Serialization**: Being a standard Java record, it is compatible with most JSON serialization libraries (such as Jackson or Gson) for transmission over the network.
*   **Maintainer**: Primary maintenance is handled by **sfaur**.
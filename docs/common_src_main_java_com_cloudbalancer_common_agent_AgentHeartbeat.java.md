# File: common/src/main/java/com/cloudbalancer/common/agent/AgentHeartbeat.java

## Overview

The `AgentHeartbeat` class is a Java `record` used to represent the periodic status update sent by a cloud agent to the central controller. It encapsulates critical telemetry data regarding the agent's resource availability, current workload, and operational capabilities. This data is essential for the load-balancing logic to make informed scheduling decisions.

## Public API

### `AgentHeartbeat` (Record)

The record provides the following immutable fields:

*   **`agentId`** (`String`): The unique identifier for the agent instance.
*   **`hostname`** (`String`): The network hostname of the machine running the agent.
*   **`totalCpuCores`** (`double`): The total number of CPU cores available on the host.
*   **`availableCpuCores`** (`double`): The number of CPU cores currently idle and available for new tasks.
*   **`totalMemoryMB`** (`long`): The total physical memory capacity in megabytes.
*   **`availableMemoryMB`** (`long`): The amount of free memory currently available in megabytes.
*   **`activeWorkerIds`** (`List<String>`): A list of identifiers for workers currently executing tasks on this agent.
*   **`supportedExecutors`** (`Set<ExecutorType>`): A set of `ExecutorType` enums defining the types of tasks this agent is capable of processing.
*   **`timestamp`** (`Instant`): The exact time the heartbeat was generated.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Used to define the capabilities of the agent.
*   `java.time.Instant`: Used for precise temporal tracking of the heartbeat.
*   `java.util.List`: Used to store the collection of active worker identifiers.
*   `java.util.Set`: Used to store the unique set of supported executor types.

## Usage Notes

*   **Immutability**: As a Java `record`, instances of `AgentHeartbeat` are immutable. Once created, the state cannot be modified, ensuring thread safety when passing heartbeat data across the system.
*   **Serialization**: This record is intended to be serialized (e.g., via JSON) when transmitted over the network from the agent to the controller. Ensure that your serialization framework (such as Jackson or Gson) is configured to support Java records.
*   **Frequency**: This object is expected to be instantiated and transmitted at regular intervals defined by the agent's configuration. The `timestamp` field should be used by the controller to detect stale agents or network latency issues.
*   **Validation**: While the record structure enforces the presence of these fields, it does not perform range validation (e.g., ensuring `availableCpuCores` <= `totalCpuCores`). Validation logic should be implemented in the service layer before processing the heartbeat.
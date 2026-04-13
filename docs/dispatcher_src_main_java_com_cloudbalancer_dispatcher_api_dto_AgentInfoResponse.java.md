# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AgentInfoResponse.java

## Overview

The `AgentInfoResponse` class is a Java `record` used as a Data Transfer Object (DTO) to encapsulate the current state and resource availability of a cloud balancer agent. It provides a structured representation of an agent's hardware capacity, current workload, and connectivity status, facilitating communication between the dispatcher and external monitoring or management services.

## Public API

### Fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `agentId` | `String` | The unique identifier for the agent. |
| `hostname` | `String` | The network hostname of the agent. |
| `totalCpuCores` | `double` | The total number of CPU cores available on the agent. |
| `availableCpuCores` | `double` | The number of CPU cores currently available for new tasks. |
| `totalMemoryMB` | `long` | The total system memory in megabytes. |
| `availableMemoryMB` | `long` | The amount of system memory currently available in megabytes. |
| `activeWorkerIds` | `List<String>` | A list of identifiers for workers currently active on this agent. |
| `supportedExecutors` | `List<String>` | A list of executor types or frameworks supported by this agent. |
| `lastHeartbeat` | `String` | A timestamp string indicating the last successful communication from the agent. |

## Dependencies

- `java.util.List`: Used to store collections of worker identifiers and supported executor types.

## Usage Notes

- **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its state cannot be modified.
- **Serialization**: This class is intended for use with JSON serialization libraries (such as Jackson or Gson) to transmit agent status data over RESTful APIs.
- **Data Precision**: CPU core counts are represented as `double` to accommodate fractional core allocation, while memory values are represented as `long` to ensure precision for byte-based calculations.
- **Heartbeat Format**: The `lastHeartbeat` field is provided as a `String`. Ensure that the producer of this object follows a consistent ISO-8601 or similar timestamp format for reliable parsing by consumers.
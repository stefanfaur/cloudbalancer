# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scaling/AgentInfo.java

## Overview

`AgentInfo` is a data model class used within the dispatcher service to maintain the current state and resource availability of registered worker agents. It acts as a local cache for agent metadata, including hardware specifications (CPU/Memory), supported execution environments, and active worker processes.

This class is central to the dispatcher's scaling logic, allowing the system to make informed scheduling decisions based on real-time resource availability reported by agents via heartbeats.

## Public API

### Constructor
*   `AgentInfo(String agentId, String hostname)`: Initializes a new agent record with a unique identifier and hostname. The `lastHeartbeat` is automatically set to the current time upon instantiation.

### Accessors
*   `agentId()`: Returns the unique identifier of the agent.
*   `hostname()`: Returns the hostname of the agent.
*   `totalCpuCores()`: Returns the total CPU core count.
*   `availableCpuCores()`: Returns the currently available CPU cores.
*   `totalMemoryMB()`: Returns the total system memory in Megabytes.
*   `availableMemoryMB()`: Returns the currently available memory in Megabytes.
*   `activeWorkerIds()`: Returns a list of IDs for workers currently running on this agent.
*   `supportedExecutors()`: Returns the set of `ExecutorType`s supported by this agent.
*   `lastHeartbeat()`: Returns the `Instant` of the most recent heartbeat received.

### Mutators
*   `setLastHeartbeat(Instant lastHeartbeat)`: Manually updates the last heartbeat timestamp.
*   `updateFrom(AgentHeartbeat hb)`: Updates the internal state of the `AgentInfo` object using data provided by an `AgentHeartbeat` message. This method refreshes all resource metrics and resets the `lastHeartbeat` to the current time.

## Dependencies

*   `com.cloudbalancer.common.model.ExecutorType`: Defines the types of executors supported by the agent.
*   `java.time.Instant`: Used for tracking heartbeat timestamps.
*   `java.util.List`: Used to store active worker identifiers.
*   `java.util.Set`: Used to store supported executor types.
*   `com.cloudbalancer.common.agent.AgentHeartbeat`: Used as the source of truth for updating agent state.

## Usage Notes

*   **State Synchronization**: This class is intended to be updated frequently via the `updateFrom` method as heartbeats arrive from the agent network.
*   **Scheduling Decisions**: The `availableCpuCores` and `availableMemoryMB` fields are frequently queried by the `AgentRegistry` and `AgentRuntime` components to determine the best host for new workload deployments.
*   **Lifecycle**: While `AgentInfo` tracks the state, it does not handle network communication itself; it relies on event listeners (such as `AgentEventListener`) to invoke `updateFrom` when new data is received from the message bus.
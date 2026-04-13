# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scaling/AgentRegistry.java

## Overview

The `AgentRegistry` class is a core component of the `dispatcher` module, responsible for maintaining the state of all active worker agents in the cloud cluster. It acts as a centralized, thread-safe repository that tracks agent availability, resource capacity (CPU/Memory), and health status based on incoming heartbeats.

This component is essential for the scheduling logic, as it provides the necessary data to match incoming workload requirements against the current cluster capacity.

## Public API

### `void updateAgent(AgentHeartbeat heartbeat)`
Registers a new agent or updates the status of an existing agent using the provided `AgentHeartbeat` data. If the agent is unknown, it creates a new `AgentInfo` entry.

### `void markDeadIfStale(Duration timeout)`
Iterates through all registered agents and removes those that have not sent a heartbeat within the specified `timeout` duration. This is typically invoked by a background maintenance task to prune disconnected nodes.

### `Optional<AgentInfo> selectBestHost(WorkerConfig config)`
Evaluates all currently registered agents to find the most suitable host for a given `WorkerConfig`. It filters agents based on:
- Sufficient CPU cores.
- Sufficient available memory.
- Compatibility with required executors.
The method returns the agent with the highest available CPU cores among those that meet the criteria.

### `List<AgentInfo> getAliveAgents()`
Returns an immutable snapshot of all currently registered agents.

### `Optional<AgentInfo> getAgent(String agentId)`
Retrieves the `AgentInfo` for a specific agent ID, if it exists.

### `void removeAgent(String agentId)`
Manually removes an agent from the registry.

## Dependencies

- `com.cloudbalancer.common.agent.AgentHeartbeat`: Data contract for incoming agent status updates.
- `com.cloudbalancer.common.runtime.WorkerConfig`: Configuration requirements used for host selection.
- `java.util.concurrent.ConcurrentHashMap`: Used to ensure thread-safe access to the agent registry.
- `org.slf4j.Logger`: Used for logging registration events and stale agent removal.
- `org.springframework.stereotype.Component`: Marks this class as a Spring-managed bean.

## Usage Notes

- **Thread Safety**: The registry uses `ConcurrentHashMap` and `compute` methods, making it safe for concurrent updates from multiple threads (e.g., multiple Kafka consumer threads processing heartbeats).
- **Maintenance**: The `markDeadIfStale` method does not run automatically. It must be scheduled periodically (e.g., via a `@Scheduled` task) to ensure the registry does not accumulate stale entries from agents that have disconnected abruptly.
- **Selection Logic**: The `selectBestHost` method implements a "most-available-CPU" heuristic. Ensure that the `WorkerConfig` passed to this method is fully populated to avoid unexpected filtering results.
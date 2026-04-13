# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scaling/AgentRuntime.java

## Overview

`AgentRuntime` is a core component of the `dispatcher` service, responsible for orchestrating worker lifecycle events across the distributed agent cluster. It implements the `NodeRuntime` interface to provide a unified mechanism for starting, stopping, and querying the status of workers by communicating with remote agents via Apache Kafka.

**Note:** This file is a **HOTSPOT** within the repository. It exhibits high change frequency and significant complexity due to its role as the primary bridge between the dispatcher's scaling logic and the physical agent infrastructure. Changes here carry a high risk of introducing regressions in cluster stability or worker deployment consistency.

## Public API

| Method | Description |
| :--- | :--- |
| `startWorker(WorkerConfig)` | Automatically selects the best available agent and dispatches a `StartWorkerCommand`. |
| `startWorkerOnAgent(WorkerConfig, String)` | Forces deployment of a worker to a specific, identified agent. |
| `stopWorker(String)` | Immediately terminates a worker on its assigned agent. |
| `drainAndStop(String, int)` | Signals an agent to gracefully drain a worker over a specified duration before termination. |
| `listWorkers()` | Aggregates active worker IDs from all registered agents. |
| `getPendingWorkerCount()` | Returns the number of workers currently in the provisioning state. |

## Dependencies

*   **KafkaTemplate**: Used for asynchronous communication with agents via the `agents.commands` topic.
*   **AgentRegistry**: Provides real-time state of agent capacity and health.
*   **PendingWorkerTracker**: Tracks workers that have been requested but are not yet reported as active by agents.
*   **AgentCommand**: The shared DTO library defining the communication protocol between the dispatcher and agents.

## Usage Notes

### Lifecycle Orchestration
The `AgentRuntime` does not maintain persistent state for workers itself; it relies on the `AgentRegistry` and `PendingWorkerTracker`. When a worker is started, it is immediately marked as "pending" to prevent race conditions where the dispatcher might attempt to schedule another worker on an agent that hasn't yet reported the new worker's resource consumption.

### Error Handling & Kafka
Communication is performed asynchronously. If `kafkaTemplate.send()` fails, the method logs the error and returns `false`. Callers should implement retry logic or handle the failure state to ensure the system does not enter an inconsistent state where the dispatcher believes a command was sent but the agent never received it.

### Implementation Pitfalls
1.  **Stale State**: `listWorkers()` relies on `agentRegistry.getAliveAgents()`. If the registry is lagging, the list may be incomplete.
2.  **Targeted Deployment**: When using `startWorkerOnAgent`, the method performs a manual capacity check. If the agent's state changes between the check and the command processing, the agent may reject the command.
3.  **Worker Info**: The `getWorkerInfo` method currently returns `null`. This is intentional, as worker metadata is managed by a separate `WorkerRegistryService`. Do not rely on `AgentRuntime` for detailed worker configuration data.

### Example: Graceful Shutdown
To trigger a graceful shutdown of a worker with a 30-second drain period:

```java
// Assuming agentRuntime is injected
agentRuntime.drainAndStop("worker-123", 30);
```

This will dispatch a `StopWorkerCommand` with the `drain` flag set to `true`, allowing the agent to complete pending tasks before shutting down the process.
# File: worker-agent/src/main/java/com/cloudbalancer/agent/kafka/AgentCommandListener.java

## Overview

The `AgentCommandListener` is a core component of the `worker-agent` service, responsible for processing control commands received via Apache Kafka. It acts as the bridge between the central control plane (Dispatcher) and the local container runtime.

**⚠️ HOTSPOT WARNING:** This file is a high-activity component with significant complexity. It handles critical lifecycle operations (starting/stopping workers) and is a primary point of failure for agent operations. Changes to this class should be thoroughly tested, as it directly impacts the stability of the worker node's container management.

## Public API

The class is designed as a Spring `@Component` and does not expose a traditional Java API. Instead, it interacts with the system via the following Kafka-driven interface:

*   **`onCommand(String message)`**: The primary entry point annotated with `@KafkaListener`. It consumes JSON-serialized `AgentCommand` objects from the `agents.commands` topic.
    *   **Filtering**: It only processes messages where the `agentId` matches the local `AgentProperties.getId()`.
    *   **Routing**: Dispatches commands to `handleStart` or `handleStop` based on the command type.

## Dependencies

*   **`KafkaTemplate<String, String>`**: Used for publishing `AgentEvent` updates and forwarding `DrainCommand` signals.
*   **`ContainerManager`**: The abstraction layer for interacting with the underlying container runtime (e.g., Docker/containerd).
*   **`AgentProperties`**: Provides configuration, specifically the unique `agentId` used for message filtering.
*   **`ScheduledExecutorService`**: A dedicated single-thread executor (`agent-drain-scheduler`) used to handle delayed worker termination during drain operations.

## Usage Notes

### Command Processing Flow
1.  **Start Command**: When a `StartWorkerCommand` is received, the listener invokes `containerManager.startWorker`. Upon success, it broadcasts a `WorkerStartedEvent`; upon failure, it broadcasts a `WorkerStartFailedEvent`.
2.  **Stop Command**: 
    *   **Immediate Stop**: If `drain` is false, `doStop` is called immediately.
    *   **Drain Stop**: If `drain` is true, the listener publishes a `DrainCommand` to the `workers.commands` topic and schedules a delayed execution of `doStop` using the `drainTimeSeconds` parameter.

### Implementation Details & Pitfalls
*   **Concurrency**: The `drainScheduler` is a daemon thread pool. Ensure that the `ContainerManager` implementation is thread-safe, as `doStop` may be invoked asynchronously after a delay.
*   **Error Handling**: All operations are wrapped in `try-catch` blocks to prevent a single malformed command or container failure from crashing the Kafka consumer thread.
*   **Eventual Consistency**: The system relies on asynchronous events (`AgentEvent`) to report status back to the control plane. If the Kafka broker is unreachable, the agent may lose synchronization with the dispatcher.
*   **Resource Management**: The `drainScheduler` is initialized at class instantiation. In environments with high churn, monitor the thread count and task queue to ensure no memory leaks occur during extended uptime.

### Example: Triggering a Worker Start
To start a worker, send a JSON payload to the `agents.commands` topic:
```json
{
  "type": "StartWorkerCommand",
  "agentId": "agent-1",
  "workerId": "worker-abc-123",
  "cpuCores": 2,
  "memoryMB": 1024
}
```
The listener will validate the `agentId`, invoke the `ContainerManager`, and publish a `WorkerStartedEvent` to `agents.events` upon completion.
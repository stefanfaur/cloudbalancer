# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/kafka/AgentEventListener.java

## Overview

The `AgentEventListener` is a core component of the `dispatcher` service, responsible for processing asynchronous events received from remote `worker-agent` instances via Apache Kafka. It acts as the primary bridge between the agent fleet and the dispatcher's internal state management systems (`AgentRegistry`, `WorkerRegistryService`, and `PendingWorkerTracker`).

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Modifications to this class can significantly impact the stability of the cluster's worker lifecycle management. Ensure rigorous testing when changing event handling logic or cross-referencing thresholds.

## Public API

The class exposes several `@KafkaListener` annotated methods that act as entry points for Kafka topics:

*   **`onHeartbeat(String message)`**: Consumes from `agents.heartbeat`. Updates the `AgentRegistry` with the latest agent status and triggers a reconciliation process (`crossReferenceAgentWorkers`) to ensure the dispatcher's view of active workers matches the agent's reported state.
*   **`onAgentEvent(String message)`**: Consumes from `agents.events`. Dispatches specific worker lifecycle events (Started, StartFailed, Stopped, StopFailed) to private handler methods.
*   **`onAgentRegistration(String message)`**: Consumes from `agents.registration`. Logs and acknowledges the registration of new agents in the cluster.

## Dependencies

This component relies on several internal services to maintain system consistency:

*   **`AgentRegistry`**: Tracks the health and metadata of connected agents.
*   **`PendingWorkerTracker`**: Manages the state of workers currently in the process of being scheduled or initialized.
*   **`WorkerRegistryService`**: The source of truth for worker health states and capabilities.
*   **`EventPublisher`**: Used to broadcast internal events (e.g., `WorkerRegisteredEvent`) to other system components.
*   **`WorkerFailureHandler`**: Orchestrates recovery or cleanup logic when a worker is detected as dead.

## Usage Notes

### Reconciliation Logic
The `crossReferenceAgentWorkers` method is critical for cluster consistency. It compares the workers known to the dispatcher against the list of active workers reported by an agent in its heartbeat.
*   **Grace Period**: Workers registered within the last 30 seconds are ignored by the reconciliation logic to prevent race conditions during startup.
*   **Dead Marking**: If a worker is known to the dispatcher but missing from the agent's heartbeat (and is not in a `DEAD` or `STOPPING` state), the dispatcher marks it as `DEAD` and triggers the `WorkerFailureHandler`.

### Event Handling Lifecycle
When a `WorkerStartedEvent` is received:
1.  The `PendingWorkerTracker` resolves the pending request.
2.  A default `WorkerCapabilities` profile is assigned (Simulated, Shell, Docker).
3.  The worker is registered in the `WorkerRegistryService` as `HEALTHY`.
4.  A `WorkerRegisteredEvent` is published to the `workers.registration` topic.

### Potential Pitfalls
*   **Deserialization Errors**: The class uses `JsonUtil` to parse incoming messages. If the schema of `AgentEvent` or `AgentHeartbeat` changes without updating the dispatcher, the listener will log an error and drop the message, potentially leading to stale state.
*   **Reconciliation Storms**: If an agent reports an empty worker list due to a local failure, the `crossReferenceAgentWorkers` method will mark all workers on that agent as `DEAD`. Ensure that agents are configured to report state accurately before triggering mass-death events.
*   **Kafka Group ID**: The listener uses `groupId = "dispatcher-agents"`. Ensure that multiple dispatcher instances are configured correctly to avoid duplicate processing or partition starvation.
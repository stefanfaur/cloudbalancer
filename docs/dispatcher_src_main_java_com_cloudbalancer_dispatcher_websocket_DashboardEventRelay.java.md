# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/websocket/DashboardEventRelay.java

## Overview

The `DashboardEventRelay` is a critical component of the CloudBalancer dispatcher service. It acts as a bridge between the system's internal Kafka event bus and the real-time dashboard frontend. By listening to various Kafka topics, it consumes system events, processes them, and broadcasts them to connected clients via the `DashboardWebSocketHandler`.

**Important**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it handles the serialization and relaying of nearly all system-wide events (tasks, workers, and scaling), any logic errors here can lead to stale or incorrect data on the dashboard, or performance degradation due to excessive WebSocket broadcasting.

## Public API

The `DashboardEventRelay` is a Spring `@Component` that automatically registers Kafka listeners upon application startup.

### Constructors
*   `DashboardEventRelay(DashboardWebSocketHandler handler, TaskService taskService)`: Initializes the relay with the required WebSocket handler for broadcasting and the `TaskService` for retrieving task metadata.

### Kafka Listener Methods
All methods are annotated with `@KafkaListener(groupId = "dashboard-relay")`.

*   `onTaskResult(String message)`: Listens to `tasks.results`. Maps incoming JSON to `TaskResult`, fetches the full `TaskEnvelope` via `TaskService`, and broadcasts a `TASK_UPDATE`.
*   `onTaskEvent(String message)`: Listens to `tasks.events`. Handles `TaskStateChangedEvent`, `TaskCompletedEvent`, and `TaskAssignedEvent` by fetching the associated `TaskEnvelope` and broadcasting a `TASK_UPDATE`.
*   `onWorkerHeartbeat(String message)`: Listens to `workers.heartbeat`. Broadcasts `WORKER_STATE` containing the worker ID and health status.
*   `onWorkerMetrics(String message)`: Listens to `workers.metrics`. Broadcasts `WORKER_UPDATE` with raw metrics data.
*   `onScalingEvent(String message)`: Listens to `system.scaling`. Broadcasts `SCALING_EVENT` directly to the dashboard.

## Dependencies

*   **`DashboardWebSocketHandler`**: Manages active WebSocket sessions and performs the actual broadcast to clients.
*   **`TaskService`**: Provides the business logic to retrieve `TaskEnvelope` objects based on task IDs.
*   **`com.cloudbalancer.common.event.*`**: Contains the event schemas (e.g., `ScalingEvent`, `WorkerMetricsEvent`).
*   **`com.cloudbalancer.common.util.JsonUtil`**: Used for deserializing Kafka message payloads.
*   **Spring Kafka**: Provides the `@KafkaListener` infrastructure.

## Usage Notes

### Implementation Rationale
The relay uses a "fetch-and-broadcast" pattern for task events. Since Kafka events often contain only minimal identifiers (like `taskId`), the relay queries the `TaskService` to enrich the event with the full `TaskEnvelope` before pushing it to the dashboard. This ensures the frontend always has the most current state of the task.

### Potential Pitfalls
1.  **Deserialization Failures**: The relay uses a generic `try-catch` block for each listener. If a message schema changes in a breaking way, the relay will log a warning and drop the event, potentially causing the dashboard to miss updates.
2.  **TaskService Latency**: Because `onTaskResult` and `onTaskEvent` call `taskService.getTask()`, high latency in the task database or service layer will delay dashboard updates.
3.  **Broadcasting Overhead**: The `handler.broadcast` method is called for every event. In a high-throughput system, this can lead to WebSocket saturation. Ensure that the dashboard frontend is optimized to handle high-frequency updates.

### Example: Adding a New Event Type
To add a new event type to the dashboard:
1.  Define the event class in `com.cloudbalancer.common.event`.
2.  Add a new `@KafkaListener` method to `DashboardEventRelay` specifying the new topic.
3.  Use `JsonUtil.mapper().readValue(message, NewEvent.class)` to deserialize.
4.  Call `handler.broadcast("NEW_EVENT_TYPE", event)` to push the data to the client.
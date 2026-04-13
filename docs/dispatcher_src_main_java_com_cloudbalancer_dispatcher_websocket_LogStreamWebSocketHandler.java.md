# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/websocket/LogStreamWebSocketHandler.java

## Overview

`LogStreamWebSocketHandler` is a Spring-based WebSocket handler responsible for managing real-time log streaming from cloud tasks to connected clients. It acts as a central hub that maps specific task IDs to active WebSocket sessions, allowing the system to broadcast log updates to all subscribers interested in a particular task.

The handler maintains a thread-safe registry of active subscriptions and ensures that log data is routed only to sessions associated with the corresponding task ID extracted from the connection URI.

## Public API

### `afterConnectionEstablished(WebSocketSession session)`
Invoked when a new WebSocket connection is opened. It attempts to extract the `taskId` from the connection URI. If successful, the session is registered to the task's subscription set; otherwise, the connection is closed with a `BAD_DATA` status.

### `afterConnectionClosed(WebSocketSession session, CloseStatus status)`
Invoked when a WebSocket connection is terminated. It removes the session from the subscription registry for the associated task. If no further sessions remain for that task, the task entry is removed from the registry.

### `broadcast(UUID taskId, String logLine)`
Sends a log message to all active WebSocket sessions currently subscribed to the specified `taskId`. This method iterates through the set of sessions, verifies their connection status, and performs the transmission.

### `extractTaskId(WebSocketSession session)`
A private helper method that parses the `taskId` from the WebSocket connection URI. It expects the URI to follow the pattern `/api/tasks/{taskId}/logs/stream`.

## Dependencies

- `org.springframework.web.socket.handler.TextWebSocketHandler`: Base class for handling text-based WebSocket messages.
- `java.util.concurrent.ConcurrentHashMap`: Used for managing thread-safe storage of task-to-session mappings.
- `org.slf4j.Logger`: Used for logging connection events and transmission errors.

## Usage Notes

- **URI Pattern**: The handler strictly expects the WebSocket connection URI to contain the `taskId` at the 4th segment (index 3) of the path (e.g., `/api/tasks/550e8400-e29b-41d4-a716-446655440000/logs/stream`). Connections failing this pattern will be rejected.
- **Thread Safety**: The class uses `ConcurrentHashMap` and `ConcurrentHashMap.newKeySet()` to ensure that multiple concurrent connections and broadcasts do not cause race conditions or `ConcurrentModificationException` errors.
- **Error Handling**: If a broadcast fails for a specific session (e.g., due to an `IOException`), the error is logged, but the broadcast continues for other sessions subscribed to the same task.
- **Lifecycle**: This component is managed by the Spring container as a `@Component`. It should be registered in the `WebSocketConfigurer` configuration to map it to the appropriate endpoint.
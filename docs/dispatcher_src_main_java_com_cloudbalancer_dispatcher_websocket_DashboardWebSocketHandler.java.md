# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/websocket/DashboardWebSocketHandler.java

## Overview

`DashboardWebSocketHandler` is a Spring-based WebSocket handler responsible for providing real-time updates to the system dashboard. It maintains a registry of active WebSocket sessions and broadcasts system state changes—such as worker health and task queue metrics—to connected clients.

**Note:** This file is a **HOTSPOT** within the `dispatcher` module. It exhibits high change frequency and complexity, serving as a critical communication bridge between the backend state and the user interface. Modifications to this class should be handled with extreme care to avoid breaking real-time monitoring capabilities.

## Public API

| Method | Description |
| :--- | :--- |
| `broadcast(String type, Object payload)` | Sends a JSON-serialized message to all currently connected WebSocket sessions. |
| `getSessionCount()` | Returns the current number of active dashboard connections. |
| `afterConnectionEstablished(WebSocketSession)` | Registers a new session and triggers the delivery of an initial system snapshot. |
| `afterConnectionClosed(WebSocketSession, CloseStatus)` | Removes a session from the registry upon disconnection. |

## Dependencies

*   **`WorkerRegistryService`**: Used to retrieve the current health and load status of all registered workers.
*   **`TaskRepository`**: Used to query the current distribution of tasks across various states (`RUNNING`, `QUEUED`, etc.).
*   **`JsonUtil`**: A utility for serializing payload maps into JSON format for transmission.
*   **`ConcurrentHashMap`**: Used to manage the thread-safe set of active `WebSocketSession` objects.

## Usage Notes

### Implementation Rationale
The handler uses a `ConcurrentHashMap.newKeySet()` to store active sessions, ensuring thread safety during concurrent connection/disconnection events. When a client connects, the `sendInitialSnapshot` method is invoked to populate the dashboard immediately, preventing the UI from waiting for the next broadcast event.

### Error Handling
*   **Serialization**: If `JsonUtil` fails to serialize a payload, the `broadcast` method logs an error and aborts the operation for that specific message to prevent system-wide instability.
*   **Transmission**: The `broadcast` method iterates through sessions and performs an `isOpen()` check before sending. If a specific session fails during transmission, the error is logged, but the broadcast continues to other healthy sessions.

### Multi-Step Usage Example
To push a custom update to all dashboard users:

1.  **Define the payload**: Create a `Map` or POJO containing the data to be sent.
2.  **Invoke broadcast**:
    ```java
    // Example: Broadcasting a worker status update from a service
    dashboardWebSocketHandler.broadcast("WORKER_UPDATE", workerStatusMap);
    ```
3.  **Client-side handling**: The client should expect a JSON object with a `type` field (e.g., `"WORKER_UPDATE"`) and a `payload` field containing the data.

### Known Pitfalls
*   **Blocking Operations**: The `broadcast` method executes synchronously across all sessions. If the number of connected clients grows significantly, broadcasting large payloads may introduce latency. Consider offloading to an asynchronous task executor if performance degrades.
*   **Session Leaks**: While `afterConnectionClosed` handles standard disconnections, ensure that network-level timeouts are configured in the `WebSocketConfig` to clean up "zombie" sessions that may not have triggered a clean close.
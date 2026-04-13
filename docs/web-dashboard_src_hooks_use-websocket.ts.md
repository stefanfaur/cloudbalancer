# File: web-dashboard/src/hooks/use-websocket.ts

## Overview

The `useWebSocket` hook provides a robust, real-time communication layer between the web dashboard and the backend server. It manages a persistent WebSocket connection, handles automatic reconnection with exponential backoff, and integrates directly with `@tanstack/react-query` to trigger data invalidations based on server-sent events.

**Note:** This file is a **critical system hotspot**. It is frequently modified and handles complex asynchronous state synchronization. Changes to this file can impact the real-time data consistency of the entire dashboard.

## Public API

### `useWebSocket()`

The primary hook used to initialize and manage the WebSocket lifecycle.

**Returns:**
- `isConnected` (boolean): Indicates if the WebSocket connection is currently active.
- `isReconnecting` (boolean): Indicates if the hook is currently in a backoff/reconnection state.

## Dependencies

- **`@tanstack/react-query`**: Used for invalidating cache keys when server updates are received.
- **`useAuth`**: Provides the `accessToken` required for secure WebSocket authentication and the `isAuthenticated` state to gate the connection.
- **`useAlerts`**: Used to push system-wide notifications received via the `ALERT` message type.
- **`@/api/types`**: Provides the `WsMessage` interface for type-safe message handling.

## Usage Notes

### Implementation Rationale
*   **Exponential Backoff**: To prevent server hammering during outages, the hook implements an exponential backoff strategy starting at 1s, doubling on each failure, and capping at 30s.
*   **Invalidation Debouncing**: To prevent excessive re-fetching when multiple messages arrive in rapid succession, the hook uses a `DEBOUNCE_MS` (500ms) window to batch `queryClient.invalidateQueries` calls.
*   **Cleanup**: The hook ensures that all timers (reconnect and debounce) and the WebSocket connection itself are properly disposed of when the component unmounts or authentication state changes.

### Potential Pitfalls
*   **Memory Leaks**: Ensure that `useWebSocket` is not called in components that mount/unmount frequently, as this will trigger repeated connection attempts. It is intended to be used at the layout or root level.
*   **Message Handling**: The `handleMessage` function is wrapped in `useCallback` to prevent unnecessary effect re-runs. Adding new message types requires updating the `switch` statement and ensuring the corresponding `queryKey` is correctly invalidated.

### Example Usage

```tsx
import { useWebSocket } from "@/hooks/use-websocket";

function DashboardLayout({ children }) {
  const { isConnected, isReconnecting } = useWebSocket();

  return (
    <div>
      {isReconnecting && <StatusBanner>Reconnecting...</StatusBanner>}
      {children}
    </div>
  );
}
```

### Supported Message Types
The hook listens for the following `WsMessage` types to trigger updates:
1.  **`TASK_UPDATE`**: Invalidates `tasks` and `task` queries.
2.  **`WORKER_UPDATE`**: Invalidates `worker-snapshots` and `cluster-metrics`.
3.  **`WORKER_STATE`**: Invalidates `worker-snapshots`.
4.  **`SCALING_EVENT`**: Invalidates `scaling-status`.
5.  **`ALERT`**: Triggers a UI notification via `addAlert`.
6.  **`INITIAL_SNAPSHOT`**: Performs a full refresh of `worker-snapshots`, `cluster-metrics`, and `tasks`.
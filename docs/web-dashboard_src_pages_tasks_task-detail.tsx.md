# File: web-dashboard/src/pages/tasks/task-detail.tsx

## Overview

`task-detail.tsx` is a critical UI component in the `web-dashboard` responsible for rendering the comprehensive status, lifecycle, and diagnostic information for a specific task. It serves as the primary interface for monitoring task execution, viewing logs in real-time, analyzing resource utilization, and performing lifecycle operations (cancellation/retries).

**⚠️ HOTSPOT WARNING:** This file is identified as a high-complexity, high-frequency change area. It integrates multiple complex hooks, WebSocket connections for log streaming, and data visualization components. Modifications to this file carry a high risk of regression in task monitoring stability.

## Public API

### `executeAction()`
An internal asynchronous function triggered by user confirmation in the UI.
- **Logic**: Checks the `confirmAction` state ("cancel" or "retry").
- **Side Effects**: Invokes `bulkCancel.mutateAsync` or `bulkRetry.mutateAsync` using the current task ID.
- **State Management**: Resets `confirmAction` to `null` upon completion.

### Components
- **`LogViewer`**: A specialized component that manages a WebSocket connection to stream task logs. It handles auto-scrolling and differentiates between `stdout` and `stderr` streams.
- **`ResourceCharts`**: A memoized component using `recharts` to visualize CPU and Memory usage over the task's execution window.

## Dependencies

This component relies heavily on the internal API and UI library ecosystem:
- **API Hooks**: `useTask`, `useTaskLogs`, `useBulkCancel`, `useBulkRetry` (from `@/api/tasks`) and `useWorkerHistory` (from `@/api/workers`).
- **Authentication**: `useAuth` for retrieving the `accessToken` required for WebSocket log streaming.
- **UI Components**: A suite of Radix-UI based components (Dialog, Tabs, Table, Card) located in `@/components/ui/`.
- **Visualization**: `recharts` for rendering resource metrics.

## Usage Notes

### Log Streaming
The `LogViewer` component automatically establishes a WebSocket connection when the task state is `RUNNING`.
- **Connection URL**: Derived from `import.meta.env.VITE_WS_URL`.
- **Authentication**: The `accessToken` from `useAuth` is passed as a query parameter. Ensure the backend supports token-based authentication for WebSocket upgrades.
- **Pitfall**: If the WebSocket fails to connect, the component falls back to displaying static logs fetched via `useTaskLogs`.

### Resource Monitoring
The `ResourceCharts` component requires `workerId`, `from`, and `to` timestamps.
- If a task has not yet been assigned to a worker, or if historical data is missing, the component gracefully renders a "No resource data" message.
- The chart resolution is hardcoded to `1m` (one-minute buckets) via the `useWorkerHistory` hook.

### Task Operations
- **Cancellable States**: `RUNNING`, `ASSIGNED`, `PROVISIONING`, `QUEUED`.
- **Retriable States**: `FAILED`, `TIMED_OUT`.
- **Workflow**:
  1. User clicks an action button (e.g., "Cancel").
  2. A `Dialog` is opened, setting the `confirmAction` state.
  3. Upon confirmation, `executeAction` performs the mutation.
  4. The UI automatically refreshes via React Query's cache invalidation (handled by the underlying `useBulk...` hooks).

### Troubleshooting
- **Missing Data**: If the task detail fails to load, the component provides a "Retry" button that triggers a manual `refetch()` of the task data.
- **Performance**: The `ResourceCharts` component is wrapped in `memo` to prevent unnecessary re-renders during parent state updates. When modifying the chart logic, ensure that `chartData` memoization remains efficient to avoid UI jank.
# File: web-dashboard/src/pages/workers/worker-detail.tsx

## Overview

`worker-detail.tsx` is a primary dashboard component responsible for rendering the detailed view of an individual worker node. It provides real-time monitoring, historical performance metrics, task tracking, and administrative tag management.

**⚠️ HOTSPOT WARNING:** This file is identified as a high-activity hotspot with significant complexity. It integrates multiple API hooks, complex state management for UI components, and real-time data visualization. Changes to this file should be approached with caution, as it is a critical interface for system observability.

## Public API

### Functions

*   **`deriveHealth(w: { cpuUsagePercent: number }): WorkerHealthState`**
    *   Determines the health status of a worker based on its CPU utilization.
    *   **Logic**: Returns `"SUSPECT"` if `cpuUsagePercent` exceeds 95%; otherwise, returns `"HEALTHY"`.

*   **`addTag()`**
    *   Internal handler for the `TagEditor` component to append a new tag to the worker's metadata.
    *   **Side Effects**: Triggers an asynchronous update via `useUpdateWorkerTags`.

*   **`removeTag(tag: string)`**
    *   Internal handler for the `TagEditor` component to remove a specific tag.
    *   **Side Effects**: Triggers an asynchronous update via `useUpdateWorkerTags`.

## Dependencies

This component relies on several internal API modules and UI primitives:

*   **API Hooks**: `@/api/workers` (snapshots/history), `@/api/tasks` (task tracking), and `@/api/admin` (tag management).
*   **UI Components**: Custom components including `HealthBadge`, `StatusBadge`, and `CpuGauge`.
*   **Visualization**: `recharts` for rendering CPU and Heap memory usage history.
*   **Utilities**: `date-fns` for relative timestamp formatting.

## Usage Notes

### Implementation Details
*   **State Management**: The component uses React's `useState` for local tag management and `memo` for `WorkerCharts` to prevent unnecessary re-renders during parent updates.
*   **Data Fetching**: It utilizes `useWorkerSnapshots` to locate the specific worker by ID from the URL parameters. If the worker is not found, it gracefully renders a "not found" state with navigation back to the worker list.
*   **Admin Features**: The `TagEditor` is currently gated by an `isAdmin` constant (hardcoded to `true` in the current implementation). Ensure this is integrated with your actual authentication provider before exposing it in production.

### Common Pitfalls
*   **Chart Data Availability**: The `WorkerCharts` component will display a placeholder message if the worker has not yet reported history metrics. This is expected behavior but may confuse users if the API is slow to populate.
*   **API Latency**: Because this page aggregates data from multiple endpoints (snapshots, history, and tasks), loading states are handled individually. The UI may appear "staggered" as different sections (e.g., the task table vs. the header) resolve.
*   **Tag Synchronization**: The `TagEditor` performs optimistic updates. If the `updateTags.mutateAsync` call fails, the UI state may become desynchronized from the server. Consider adding error handling to the `addTag` and `removeTag` functions to revert state on failure.

### Example: Adding a Tag
1.  Navigate to the Worker Detail page.
2.  Locate the **Tags** section.
3.  Type the desired tag in the input field.
4.  Press `Enter` or click the `+` icon.
5.  The tag will appear as a badge, and the `useUpdateWorkerTags` mutation will be triggered to persist the change to the backend.
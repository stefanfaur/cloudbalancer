# File: web-dashboard/src/pages/workers/worker-list.tsx

## Overview

The `worker-list.tsx` component is a critical UI module in the web dashboard responsible for rendering the centralized registry of active workers. It provides a real-time, sortable table view that merges static registry data with dynamic performance metrics (CPU, memory, and task load).

**⚠️ HOTSPOT WARNING:** This file is identified as a high-activity, high-complexity component. It serves as a primary interface for monitoring system health and performing administrative actions (killing workers). Changes to this file carry a high risk of regression in system observability and administrative control.

## Public API

### Types
*   **`SortField`**: Defines the columns available for sorting: `"workerId" | "health" | "cpu" | "memory" | "activeTasks"`.
*   **`SortDir`**: Defines the sort direction: `"asc" | "desc"`.
*   **`MergedWorker`**: The internal data structure representing a worker, combining registry metadata with real-time snapshot metrics.

### Functions
*   **`toggleSort(field: SortField)`**: Updates the current sort state. If the selected field is already active, it toggles the direction; otherwise, it resets to ascending order.
*   **`sortIndicator(f: SortField)`**: Returns a string representation (`↑` or `↓`) to visually indicate the active sort column and direction in the UI.

## Dependencies

*   **API Layer**: Relies on `useWorkerRegistry` (admin) and `useWorkerSnapshots` (workers) for data fetching.
*   **Components**: Utilizes shared UI components including `HealthBadge`, `CpuGauge`, `ErrorCard`, and standard table/dialog primitives.
*   **Icons**: Uses `lucide-react` for visual indicators (`Server`, `Skull`).

## Usage Notes

### Data Merging Logic
The component performs a client-side join between the `registry` (source of truth for existence) and `metrics` (supplementary performance data). 
*   **Pitfall**: If a worker exists in the registry but has no corresponding entry in the metrics snapshot, the UI gracefully falls back to `null` values, displaying a dash (`—`) in the table.
*   **Rationale**: This approach ensures that even if metrics collection fails or lags, the list of workers remains visible and actionable.

### Sorting Implementation
The sorting logic is computed on every render using `useMemo`.
*   **Edge Cases**: The sort function handles `null` values for CPU and Memory by treating them as `0` during comparison. This ensures that workers without reported metrics are consistently grouped at the bottom of the list when sorting by performance metrics.

### Administrative Actions (Kill Worker)
The "Kill" action is protected by a `Dialog` confirmation flow.
*   **Safety**: The "Kill" button is conditionally rendered only if the worker's `healthState` is not `"DEAD"` or `"STOPPING"`.
*   **Interaction**: The `onClick` handlers for the `Dialog` and the `TableRow` are carefully managed with `e.stopPropagation()` to prevent navigating to the worker detail page when a user clicks the "Kill" button.

### Multi-Step Usage Example: Managing a Stalled Worker
1.  **Identify**: Sort the table by `activeTasks` or `cpu` to find workers that are potentially stalled or overloaded.
2.  **Verify**: Check the `HealthBadge` status. If a worker is in an unhealthy state, click the row to navigate to the detail page for further investigation.
3.  **Remediate**: If the worker is unresponsive, return to the `WorkerList`, click the **Skull (Kill)** icon, and confirm the action in the dialog to force-stop the process and trigger task re-queuing.
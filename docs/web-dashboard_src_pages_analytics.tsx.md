# File: web-dashboard/src/pages/analytics.tsx

## Overview

The `web-dashboard/src/pages/analytics.tsx` file serves as the primary administrative dashboard for monitoring cluster performance, worker load distribution, and operational costs. It provides a real-time visualization of system metrics, including throughput, latency, and CPU utilization.

**Note:** This file is a **HOTSPOT** within the repository, ranking in the top 25% for both change frequency and complexity. It is a high-risk area for bugs; changes to data fetching logic or UI components here can significantly impact the observability of the entire cluster.

## Public API

### `cpuColor(pct: number): string`
A utility function that maps CPU percentage values to Tailwind CSS background color classes.

*   **Parameters**: `pct` (number) - The CPU utilization percentage (0-100).
*   **Returns**: A string representing a Tailwind color class.
*   **Logic**:
    *   `>= 80%`: `bg-red-500`
    *   `>= 60%`: `bg-amber-500`
    *   `>= 40%`: `bg-blue-500`
    *   `>= 20%`: `bg-sky-400`
    *   `< 20%`: `bg-slate-700`

## Dependencies

This page relies on several internal APIs and UI components:

*   **API Hooks**:
    *   `@/api/admin`: `useStrategy` for retrieving current load-balancing logic.
    *   `@/api/workers`: `useClusterMetrics`, `useWorkerSnapshots`, and `useWorkerHistory` for real-time data streaming.
*   **UI Components**: Utilizes standard dashboard primitives including `Card`, `Badge`, `Skeleton`, `Input`, and `Label` from `@/components/ui`.
*   **Icons**: `lucide-react` for visual indicators (`BarChart3`, `DollarSign`).

## Usage Notes

### Implementation Rationale
The page is divided into three functional sections:
1.  **Strategy Section**: Displays the active load-balancing strategy and high-level cluster performance metrics (throughput, latency, queue wait times).
2.  **Worker Load Heatmap**: Visualizes individual worker performance over the last hour using 5-minute buckets. This is critical for identifying "hot" nodes or uneven distribution.
3.  **Cost Simulator**: An interactive tool allowing administrators to estimate daily operational costs based on configurable worker-hour pricing.

### Potential Pitfalls
*   **Data Freshness**: The `HeatmapRow` component generates timestamps using `new Date().toISOString()`. Because this is memoized, ensure that the polling interval for `useWorkerHistory` is sufficient to prevent stale data visualization.
*   **Performance**: The `LoadHeatmap` iterates over all active worker snapshots. In clusters with a high number of workers, this may cause layout shifts or rendering lag. The use of `Skeleton` components during loading states is mandatory to maintain UX.
*   **Cost Calculation**: The `CostSimulator` performs client-side arithmetic. If `cluster.throughputPerMinute` is 0, the "Cost per Task" calculation defaults to 0 to avoid `NaN` errors.

### Example: Adding a New Metric
To add a new metric to the Strategy section, follow these steps:
1.  Update the `useClusterMetrics` hook in `@/api/workers.ts` to include the new data field.
2.  Add the corresponding UI element within the `Analytics` component's `section` for "Current Strategy".
3.  Wrap the new element in a `div` with `text-xs text-slate-500` for the label and `text-sm font-mono` for the value to maintain visual consistency.

```tsx
// Example: Adding a new metric display
<div>
  <p className="text-xs text-slate-500">New Metric</p>
  <p className="text-sm font-mono">{cluster.data.newMetricValue}</p>
</div>
```
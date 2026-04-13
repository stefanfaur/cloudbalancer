# File: web-dashboard/src/pages/cluster-overview.tsx

## Overview

`web-dashboard/src/pages/cluster-overview.tsx` is the primary dashboard view for the cluster management system. It provides a real-time visualization of cluster-wide performance metrics, including CPU utilization, task throughput, and individual worker health status.

**⚠️ HOTSPOT WARNING:** This file is classified as a high-complexity, high-frequency change area. It serves as a central hub for data visualization and state management for the entire cluster. Modifications to this file carry a high risk of regression in UI stability and data synchronization.

## Public API

### `useMetricsHistory(cpuPercent: number | undefined, throughput: number | undefined)`

A custom React hook that maintains a rolling buffer of cluster performance data for time-series visualization.

- **Parameters**:
  - `cpuPercent`: The current average CPU utilization percentage.
  - `throughput`: The current tasks-per-minute throughput.
- **Returns**: An array of objects `{ time: string; cpu: number; throughput: number }` containing the last 60 data points.
- **Behavior**: 
  - Automatically filters out duplicate consecutive data points to prevent unnecessary re-renders.
  - Limits the history buffer to the most recent 60 entries (sliding window).

## Dependencies

- **React Hooks**: `useState`, `useEffect`, `useRef`, `memo` for state management and performance optimization.
- **API Layer**: `useClusterMetrics` and `useWorkerSnapshots` from `@/api/workers`.
- **Visualization**: `recharts` (AreaChart, ResponsiveContainer) for rendering performance graphs.
- **UI Components**: Custom components including `KpiCard`, `HealthBadge`, `CpuGauge`, and `ErrorCard` from the `@/components` directory.
- **Icons**: `lucide-react` for dashboard iconography.

## Usage Notes

### Data Flow and Lifecycle
1. **Fetching**: The component consumes data from the backend via `useClusterMetrics` and `useWorkerSnapshots`.
2. **Aggregation**: The `useMetricsHistory` hook transforms raw stream data into a format suitable for `recharts`.
3. **Health Derivation**: The local `deriveHealth` function evaluates worker status based on CPU thresholds (`> 95%` is marked as `SUSPECT`). Note that this logic is client-side and may need to be synchronized with backend health-check definitions.

### Implementation Pitfalls
- **Re-render Performance**: The charts are wrapped in `memo` to prevent unnecessary re-renders when the parent component updates. Ensure that any data passed to these charts is memoized or stable.
- **Memory Management**: The `useMetricsHistory` hook uses a fixed-size array (`.slice(-60)`). If the polling interval for `useClusterMetrics` is significantly faster than 1 second, the time-series data will represent a very short window.
- **Error Handling**: The component includes a global error boundary check for the cluster metrics fetch. If the API fails, the entire dashboard displays an `ErrorCard` with a retry mechanism.

### Example: Extending the Dashboard
To add a new metric to the cluster overview:
1. Update the `useMetricsHistory` hook to accept the new metric parameter.
2. Add the new field to the state object inside the hook.
3. Update the `ClusterOverview` component to include a new `KpiCard` for the metric.
4. If visualization is required, add a new `Area` component to the relevant `Recharts` container.

```typescript
// Example: Adding a new metric to the history hook
function useMetricsHistory(cpu, throughput, memory) {
  // ... inside setHistory
  return [...prev, { time, cpu, throughput, memory }].slice(-60);
}
```
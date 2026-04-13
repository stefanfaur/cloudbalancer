# File: web-dashboard/src/api/workers.ts

## Overview

`web-dashboard/src/api/workers.ts` provides a set of React Query hooks designed to interface with the system's worker and cluster metrics API. It acts as the primary data-fetching layer for worker-related monitoring and administrative actions within the web dashboard.

## Public API

### `useWorkerSnapshots()`
Fetches a list of current snapshots for all active workers.
- **Returns**: A `UseQueryResult` containing an array of `WorkerMetricsSnapshot` objects.
- **Caching**: Data is considered stale after 30 seconds.

### `useWorkerHistory(id, from?, to?, bucket?)`
Retrieves historical metrics for a specific worker.
- **Parameters**:
    - `id`: The unique identifier of the worker.
    - `from` (optional): ISO timestamp for the start of the range.
    - `to` (optional): ISO timestamp for the end of the range.
    - `bucket` (optional): Time interval bucket size.
- **Returns**: A `UseQueryResult` containing an array of `WorkerMetricsBucket` objects.
- **Caching**: Data is considered stale after 60 seconds.

### `useClusterMetrics()`
Fetches high-level metrics for the entire cluster.
- **Returns**: A `UseQueryResult` containing a `ClusterMetrics` object.
- **Caching**: Data is considered stale after 30 seconds.

### `useKillWorker()`
Provides a mutation to terminate a specific worker node.
- **Returns**: A `UseMutationResult` that accepts a `workerId` string.
- **Side Effects**: Upon successful execution, it automatically invalidates the `["worker-snapshots"]` query to ensure the UI reflects the updated worker list.

## Dependencies

- **`@tanstack/react-query`**: Used for managing server state, caching, and mutations.
- **`./client`**: Imports `apiFetch` for standardized HTTP requests.
- **`./types`**: Imports core type definitions (`WorkerMetricsSnapshot`, `WorkerMetricsBucket`, `ClusterMetrics`).

## Usage Notes

- **Data Freshness**: The hooks are configured with specific `staleTime` values (30s for snapshots/cluster metrics, 60s for history) to balance UI responsiveness with server load.
- **Error Handling**: As these hooks wrap `useQuery` and `useMutation`, standard React Query error handling patterns (e.g., `isError`, `error` object) should be implemented in the consuming components.
- **Invalidation**: The `useKillWorker` mutation is tightly coupled with `useWorkerSnapshots`. If you implement custom caching strategies, ensure that `queryClient.invalidateQueries` is called appropriately if the worker list state changes.
- **Conditional Fetching**: `useWorkerHistory` is configured with `enabled: !!id`, meaning it will not trigger a network request until a valid `id` is provided.
# File: web-dashboard/src/api/tasks.ts

## Overview

`web-dashboard/src/api/tasks.ts` is a critical data-fetching layer providing React Query hooks to interact with the task management API. It handles task retrieval, logging, submission, and bulk operations (cancellation, retrying, and reprioritization).

**⚠️ HOTSPOT WARNING:** This file is identified as a high-activity hotspot with significant complexity and frequent changes. It is a high-risk area for regressions. Exercise extreme caution when modifying query keys, API endpoints, or mutation invalidation logic.

## Public API

### Interfaces
*   **`TaskFilters`**: Defines query parameters for filtering tasks, including `offset`, `limit`, `status`, `priority`, `executorType`, `workerId`, and `since`.

### Utility Functions
*   **`buildTaskParams(filters?: TaskFilters): string`**: Converts a `TaskFilters` object into a URL-encoded query string. Returns an empty string if no filters are provided.

### React Query Hooks
*   **`useTasks(filters?: TaskFilters)`**: Fetches a paginated list of tasks. Configured with a 15-second `staleTime`.
*   **`useTask(id: string)`**: Fetches details for a specific task by ID. Configured with a 10-second `staleTime`.
*   **`useTaskLogs(id: string)`**: Retrieves logs for a specific task. Configured with a 5-second `staleTime`.
*   **`useSubmitTask()`**: Mutation hook to create a new task. Automatically invalidates the `["tasks"]` query cache upon success.
*   **`useBulkCancel()`**: Mutation hook to cancel multiple tasks by ID. Invalidates the `["tasks"]` query cache.
*   **`useBulkRetry()`**: Mutation hook to retry multiple tasks by ID. Invalidates the `["tasks"]` query cache.
*   **`useBulkReprioritize()`**: Mutation hook to update the priority of multiple tasks. Invalidates the `["tasks"]` query cache.

## Dependencies

*   **`@tanstack/react-query`**: Used for state management of server-side data.
*   **`web-dashboard/src/api/client.ts`**: Provides the `apiFetch` utility for standardized HTTP requests.
*   **`web-dashboard/src/api/types.ts`**: Provides shared type definitions (`TaskPageResponse`, `TaskEnvelope`, `TaskLogsResponse`, `BulkResultEntry`, `Priority`).

## Usage Notes

### Query Cache Invalidation
All mutation hooks (`useSubmitTask`, `useBulkCancel`, etc.) are configured to invalidate the `["tasks"]` query key upon success. This ensures that the UI remains synchronized with the server state. If you add new query keys (e.g., specific filtered views), ensure that the invalidation logic is updated to prevent stale data.

### Handling Hotspot Complexity
Due to the high-risk nature of this file, follow these practices:
1.  **Type Safety**: Always use the imported types from `types.ts` when extending functionality.
2.  **Stale Time**: The `staleTime` values are tuned for specific performance requirements. Do not decrease these values without considering the impact on API load.
3.  **Error Handling**: The hooks rely on `apiFetch`. Ensure that any component consuming these hooks implements proper error boundary handling, as the hooks do not encapsulate error UI logic.

### Example: Fetching and Canceling Tasks
```tsx
// 1. Fetching tasks with filters
const { data, isLoading } = useTasks({ status: 'pending', limit: 10 });

// 2. Bulk canceling tasks
const { mutate: cancelTasks } = useBulkCancel();

const handleCancel = (ids: string[]) => {
  cancelTasks(ids, {
    onSuccess: () => console.log("Tasks canceled successfully"),
    onError: (err) => console.error("Failed to cancel tasks", err)
  });
};
```
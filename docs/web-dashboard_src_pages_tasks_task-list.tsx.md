# File: web-dashboard/src/pages/tasks/task-list.tsx

## Overview

`web-dashboard/src/pages/tasks/task-list.tsx` is a core administrative interface component responsible for displaying, filtering, and managing the lifecycle of system tasks. It provides a comprehensive dashboard for monitoring task states, priorities, and execution history.

**⚠️ HOTSPOT WARNING:** This file is identified as a high-activity hotspot (top 25% for change frequency and complexity). It is a critical path for system operations and is considered a high-risk area for regressions. Changes to this file should be accompanied by thorough integration testing.

## Public API

The component exposes several utility functions used for state management and interaction:

*   **`SortField`**: A type alias defining valid columns for sorting: `"submittedAt" | "state" | "priority"`.
*   **`SortDir`**: A type alias defining sort direction: `"asc" | "desc"`.
*   **`toggleSort(field: SortField)`**: Updates the current sort field and toggles the direction if the field is already active.
*   **`toggleSelect(id: string)`**: Adds or removes a specific task ID from the bulk-action selection set.
*   **`toggleAll()`**: Selects or deselects all currently visible tasks in the table.
*   **`updateParam(key: string, value: string)`**: Updates URL search parameters to trigger re-fetching of data, resetting pagination to the first page.
*   **`executeAction()`**: Triggers the selected bulk operation (Cancel, Retry, or Reprioritize) via the API and clears the selection state.

## Dependencies

This component relies on several internal and external libraries:

*   **API Layer**: `@/api/tasks` (Hooks for `useTasks`, `useBulkCancel`, `useBulkRetry`, `useBulkReprioritize`).
*   **UI Components**: A suite of custom components from `@/components/ui/` (Table, Dialog, Button, Checkbox, etc.).
*   **Data Utilities**: `date-fns` for relative time formatting and `@/lib/utils` for class name merging.
*   **Icons**: `lucide-react` for UI iconography.

## Usage Notes

### Filtering and Search
The component uses URL search parameters to maintain state. This allows users to share links with specific filters applied.
*   **Time Range**: Uses a `since` timestamp calculated from the `timeRange` parameter (e.g., "1h", "24h").
*   **Search**: Performs client-side filtering on the returned task list by matching ID prefixes.

### Bulk Operations
When tasks are selected, a bulk action bar appears.
1.  **Selection**: Use the checkbox in the table header or individual rows.
2.  **Action**: Choose between "Cancel", "Retry", or "Reprioritize".
3.  **Confirmation**: A `Dialog` component acts as a safety gate, requiring explicit confirmation before the `executeAction` function triggers the mutation.

### Implementation Rationale
*   **Client-side Sorting**: While filtering is handled server-side via `useTasks` parameters, sorting is performed client-side on the returned data set. This reduces server load but may lead to inconsistent sorting if the dataset exceeds the current page limit.
*   **State Management**: The component uses `useState` for UI-specific state (selection, confirmation dialogs) and `useSearchParams` for data-fetching state, ensuring the UI remains synchronized with the URL.

### Common Pitfalls
*   **Pagination Reset**: When calling `updateParam`, the `offset` is automatically reset to `0`. Ensure that any new filter additions explicitly handle this if they should not trigger a page reset.
*   **Memory Usage**: The `selected` state is a `Set<string>`. If the number of tasks per page increases significantly, ensure the UI remains performant during bulk selection operations.
*   **API Contract**: The `executeAction` function assumes that the `confirmAction` state is correctly populated. Always validate the `priority` field when performing a `reprioritize` action to avoid sending `undefined` values to the API.
# File: web-dashboard/src/pages/tasks/__tests__/task-list.test.tsx

## Overview

`web-dashboard/src/pages/tasks/__tests__/task-list.test.tsx` is the primary test suite for the `TaskList` component. It validates the rendering, data fetching, and role-based access control (RBAC) logic for the task management interface.

**Note:** This file is a **HOTSPOT**. It is frequently modified due to the evolving nature of the task management API and UI requirements. Changes here often indicate shifts in how task states, permissions, or data structures are handled in the frontend.

## Public API

### `wrapper`
A React component that provides the necessary context providers for testing components that rely on global state or routing.

- **Signature**: `function wrapper({ children }: { children: React.ReactNode })`
- **Purpose**: Wraps the component under test with:
    - `QueryClientProvider`: Manages React Query state with `retry: false` to ensure tests fail fast on data fetching errors.
    - `MemoryRouter`: Enables navigation and routing-related hooks (e.g., `useNavigate`, `useParams`) within the test environment.

## Dependencies

- **`@tanstack/react-query`**: Used for managing server state and mocking the `QueryClient`.
- **`react-router-dom`**: Used for providing routing context.
- **`@/api/tasks`**: The source of truth for task data; mocked to provide deterministic test data.
- **`@/hooks/use-auth`**: Provides authentication and authorization context; mocked to simulate different user roles (`ADMIN` vs `VIEWER`).
- **`vitest`**: The test runner and mocking framework.

## Usage Notes

### Mocking Strategy
The test suite uses `vi.mock` to isolate the `TaskList` component from external side effects:
1. **API Hooks**: `useTasks`, `useBulkCancel`, `useBulkRetry`, and `useBulkReprioritize` are mocked to return controlled data or `vi.fn()` placeholders.
2. **Auth Hook**: `useAuth` is mocked to allow toggling between `ADMIN` and `VIEWER` roles to verify UI visibility logic.
3. **Alerts Hook**: `useAlerts` is mocked to prevent errors during component mounting.

### Testing Patterns
- **Role-Based Access Control**: Tests explicitly verify that the "Submit Task" button is conditionally rendered based on the `role` returned by `useAuth`.
- **Loading States**: The suite verifies that loading skeletons are displayed when `isLoading` is true and that task data is absent during this state.
- **Data Rendering**: Uses `screen.getByText` to verify that task IDs and status badges (e.g., "RUNNING", "COMPLETED") are correctly mapped to the DOM.

### Potential Pitfalls
- **Mock Data Mismatch**: If the `Task` interface in `api/tasks.ts` changes, the `mockTasks` object in this test file must be updated immediately to prevent false negatives.
- **QueryClient Configuration**: The `wrapper` disables retries (`retry: false`). If tests are failing due to timeouts, ensure the `QueryClient` configuration in the test matches the production environment's requirements.
- **Hotspot Risk**: Because this file is a hotspot, ensure that any changes to the `TaskList` component logic are mirrored with corresponding test cases here to maintain high coverage and prevent regressions in task management workflows.
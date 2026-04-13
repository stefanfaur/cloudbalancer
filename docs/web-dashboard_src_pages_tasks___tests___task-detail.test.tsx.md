# File: web-dashboard/src/pages/tasks/__tests__/task-detail.test.tsx

## Overview

`task-detail.test.tsx` is the primary test suite for the `TaskDetail` component. It validates the rendering logic, state-based UI transitions, and role-based access control (RBAC) for the task management dashboard.

**Criticality Notice:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It serves as a high-risk area for regressions, as it governs the interface for core task lifecycle operations (Cancel, Retry). Changes to the task state machine or authorization logic must be verified against this suite.

## Public API

The test suite does not export any members. It interacts with the following internal APIs via mocks:

*   **`useTask`**: Mocked to provide controlled task state (`RUNNING`, `COMPLETED`, `FAILED`).
*   **`useAuth`**: Mocked to simulate different user roles (`ADMIN`, `VIEWER`) to verify UI security constraints.
*   **`useBulkCancel` / `useBulkRetry`**: Mocked to intercept mutation calls during interaction tests.

## Dependencies

*   **`@testing-library/react`**: Used for component rendering and DOM querying.
*   **`@tanstack/react-query`**: Provides the `QueryClientProvider` required for the `useTask` hook.
*   **`react-router-dom`**: Used to simulate the URL parameter context (`/tasks/:id`) required by the `TaskDetail` component.
*   **`vitest`**: The test runner and mocking framework.
*   **`recharts`**: Mocked to prevent rendering issues in the test environment for data visualization components.

## Usage Notes

### Testing Strategy
The suite uses a `wrapper` function to inject the necessary providers (`QueryClientProvider`, `MemoryRouter`) into the component tree. This ensures that hooks like `useTask` and `useParams` function correctly during tests.

### Common Pitfalls
1.  **Mocking Complexity**: Because `TaskDetail` relies on multiple context providers (Auth, Alerts, Query), ensure any new hooks added to `TaskDetail` are also mocked in the `beforeEach` or `vi.mock` blocks.
2.  **State Mismatch**: When testing state transitions (e.g., `RUNNING` to `FAILED`), ensure the `mockTask` object is deeply cloned or updated correctly to avoid cross-test pollution.
3.  **Recharts**: The `recharts` library is mocked to return simple `div` elements. If you are testing the visual output of charts, you must update the mock to reflect the internal structure of the chart components.

### Example: Adding a New Test Case
To test a new UI state (e.g., a "Pending" status), follow this pattern:

```typescript
it("shows specific UI for PENDING tasks", () => {
  vi.mocked(useTask).mockReturnValue({
    ...mockTask,
    data: { ...mockTask.data, state: "PENDING" },
  } as unknown as ReturnType<typeof useTask>);
  
  render(<TaskDetail />, { wrapper });
  
  expect(screen.getByText("PENDING")).toBeInTheDocument();
  // Add assertions for specific buttons or labels visible only in PENDING
});
```

### RBAC Verification
Always verify that new UI elements added to `TaskDetail` are gated by the `useAuth` role check. Use the existing `VIEWER` role test case as a template to ensure that sensitive actions are hidden from unauthorized users.
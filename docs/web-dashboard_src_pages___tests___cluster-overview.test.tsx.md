# File: web-dashboard/src/pages/__tests__/cluster-overview.test.tsx

## Overview

`cluster-overview.test.tsx` is a critical test suite for the `ClusterOverview` page component. It validates the dashboard's ability to display real-time cluster metrics and individual worker snapshots. 

**Note:** This file is a **HOTSPOT**. It is frequently modified and maintains high complexity due to its reliance on multiple mocked external services, including TanStack Query, React Router, and various custom hooks. Changes here directly impact the reliability of the cluster monitoring interface.

## Public API

### `wrapper`
A React component wrapper used to provide the necessary context for testing components that rely on global state.

- **Signature**: `function wrapper({ children }: { children: React.ReactNode })`
- **Purpose**: Injects `QueryClientProvider` (with disabled retries for deterministic testing) and `MemoryRouter` into the component tree.
- **Usage**: Used as the `wrapper` option in `testing-library`'s `render` function.

## Dependencies

- **Testing Frameworks**: `vitest`, `@testing-library/react`.
- **State Management**: `@tanstack/react-query` (for API data fetching).
- **Routing**: `react-router-dom` (`MemoryRouter`).
- **Internal Hooks**:
    - `useClusterMetrics` and `useWorkerSnapshots` from `@/api/workers`.
    - `useAlerts`, `useWebSocket`, and `useAuth` (mocked).
- **UI Components**: `recharts` (mocked to prevent SVG rendering issues in JSDOM).

## Usage Notes

### Mocking Strategy
The test suite utilizes `vi.mock` extensively to isolate the `ClusterOverview` component from backend infrastructure. 
- **API Hooks**: `useClusterMetrics` and `useWorkerSnapshots` are mocked to return controlled data objects (`mockClusterMetrics`, `mockWorkerSnapshots`).
- **Charts**: `recharts` is mocked to return simple `div` elements, as full SVG rendering is not supported in the JSDOM environment used by Vitest.

### Common Testing Patterns
To test different application states, use `vi.mocked(hookName).mockReturnValue(...)` within individual `it` blocks:

1.  **Loading State**:
    ```typescript
    vi.mocked(useClusterMetrics).mockReturnValue({ ...mockClusterMetrics, isLoading: true });
    ```
2.  **Empty State**:
    ```typescript
    vi.mocked(useWorkerSnapshots).mockReturnValue({ ...mockWorkerSnapshots, data: [] });
    ```

### Potential Pitfalls
- **JSDOM Limitations**: Since `recharts` is mocked, visual regressions in charts will not be caught by these tests. Ensure manual verification of chart rendering when modifying the `ClusterOverview` component.
- **QueryClient Configuration**: The `wrapper` disables retries (`retry: false`). If you are testing logic that depends on retry behavior, you must provide a custom `QueryClient` instance.
- **Hotspot Risk**: Because this file tests the primary dashboard view, ensure that any changes to the `ClusterOverview` API contract are reflected in both the `mockClusterMetrics` and `mockWorkerSnapshots` objects to avoid false negatives.
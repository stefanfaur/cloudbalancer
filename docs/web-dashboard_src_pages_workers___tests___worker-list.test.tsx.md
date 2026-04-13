# File: web-dashboard/src/pages/workers/__tests__/worker-list.test.tsx

## Overview

The `web-dashboard/src/pages/workers/__tests__/worker-list.test.tsx` file contains the unit and integration test suite for the `WorkerList` component. It validates the rendering of worker data, health status indicators, and empty state handling within the web dashboard.

## Public API

### `wrapper`
A React component wrapper used to provide the necessary context for testing components that rely on external state management and routing.

- **Signature**: `function wrapper({ children }: { children: React.ReactNode })`
- **Purpose**: Wraps the component under test with `QueryClientProvider` (for TanStack Query) and `MemoryRouter` (for React Router navigation).
- **Configuration**: Configures a `QueryClient` with `retry: false` to ensure tests fail immediately on data fetching errors.

## Dependencies

- **Testing Frameworks**: `vitest`, `@testing-library/react`
- **State Management**: `@tanstack/react-query`
- **Routing**: `react-router-dom`
- **Internal API**: `@/api/workers` (mocked via `useWorkerSnapshots`)
- **Internal Hooks**: `@/hooks/use-auth`, `@/hooks/use-alerts` (mocked to provide authentication and notification context)

## Usage Notes

- **Mocking Strategy**: The test suite uses `vi.mock` to intercept calls to the `useWorkerSnapshots` hook. This allows for deterministic testing of the `WorkerList` component by injecting controlled mock data (`mockWorkers`) without requiring a live backend connection.
- **Test Environment**: The `wrapper` function is essential for any test involving `WorkerList`. When adding new tests, ensure the `wrapper` is passed as an option to the `render` function:
  ```typescript
  render(<WorkerList />, { wrapper });
  ```
- **State Simulation**: To test different UI states (e.g., loading, empty, or populated), use `vi.mocked(useWorkerSnapshots).mockReturnValue(...)` before calling `render`.
- **Health Logic**: Tests verify that the component correctly interprets worker metrics (e.g., CPU usage) to display the appropriate health status badges (e.g., "HEALTHY").
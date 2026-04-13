# File: web-dashboard/src/pages/workers/__tests__/worker-detail.test.tsx

## Overview

The `worker-detail.test.tsx` file contains the unit and integration test suite for the `WorkerDetail` component. It validates the rendering of worker-specific metrics, health status, and role-based access control (RBAC) for administrative actions such as tag management.

The suite utilizes `vitest` and `@testing-library/react` to simulate the application environment, including routing, authentication states, and asynchronous data fetching via `react-query`.

## Public API

This file is a test suite and does not export any components or functions for use in the production application.

## Dependencies

The test suite relies on the following internal and external dependencies:

*   **`@testing-library/react`**: Used for rendering components and querying the DOM.
*   **`@tanstack/react-query`**: Provides the `QueryClientProvider` required to mock data fetching hooks.
*   **`react-router-dom`**: Used to simulate the URL structure (`/workers/:id`) required by the `WorkerDetail` component.
*   **`vitest`**: The test runner and mocking framework.
*   **`@/api/workers`**: Mocked to provide controlled worker snapshot data.
*   **`@/hooks/use-auth`**: Mocked to simulate different user roles (`ADMIN` vs `VIEWER`).
*   **`recharts`**: Mocked to prevent rendering issues with SVG-based charts during testing.

## Usage Notes

*   **Mocking Strategy**: The suite heavily mocks API hooks (`useWorkerSnapshots`, `useTasks`, `useUpdateWorkerTags`) to ensure tests are deterministic and do not rely on a live backend.
*   **Wrapper Pattern**: A custom `wrapper` function is provided to `render` to inject the necessary `QueryClient` and `MemoryRouter` context, ensuring the component under test has access to required providers.
*   **RBAC Testing**: The suite specifically tests the visibility of the tag editor. To test different roles, use `vi.mocked(useAuth).mockReturnValue(...)` within individual test cases to override the default authentication state.
*   **Environment**: Ensure that the `vitest` environment is configured to support `jsdom` to allow for DOM manipulation and querying.
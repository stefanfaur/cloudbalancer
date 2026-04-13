# File: web-dashboard/src/pages/__tests__/analytics.test.tsx

## Overview

The `web-dashboard/src/pages/__tests__/analytics.test.tsx` file contains the unit and integration test suite for the `Analytics` page component. It verifies that the dashboard correctly displays cluster metrics, worker snapshots, and cost simulation data by mocking external API dependencies and authentication states.

## Public API

### `wrapper`
A React component wrapper used to provide necessary context providers to the `Analytics` component during testing.

- **Signature**: `function wrapper({ children }: { children: React.ReactNode })`
- **Providers included**:
    - `QueryClientProvider`: Manages React Query state with retries disabled for deterministic testing.
    - `MemoryRouter`: Provides routing context for components that utilize `react-router-dom` hooks.

## Dependencies

- **Testing Frameworks**: `vitest`, `@testing-library/react`
- **State Management**: `@tanstack/react-query`
- **Routing**: `react-router-dom`
- **Internal Modules**: 
    - `../analytics`: The component under test.
    - `@/api/admin`: Mocked for strategy configuration.
    - `@/api/workers`: Mocked for cluster metrics and worker snapshots.
    - `@/hooks/use-auth`: Mocked to simulate an authenticated admin session.
    - `@/hooks/use-alerts`: Mocked to provide alert management context.

## Usage Notes

- **Mocking Strategy**: The test suite uses `vi.mock` to intercept network requests. If the API structure in `@/api/admin` or `@/api/workers` changes, these mocks must be updated to reflect the new data shapes to prevent test failures.
- **Test Environment**: The `wrapper` function is required as the second argument to `render()` when testing the `Analytics` page to ensure that React Query and Router hooks do not throw errors due to missing providers.
- **Cost Simulation**: The tests verify the cost simulator logic based on a hardcoded assumption of 3 workers at a rate of $0.10/hr. Any changes to the default pricing model in the `Analytics` component will require corresponding updates to the assertions in the "cost simulator calculates correctly" test case.
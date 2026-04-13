# File: web-dashboard/src/pages/tasks/__tests__/task-submit.test.tsx

## Overview

The `web-dashboard/src/pages/tasks/__tests__/task-submit.test.tsx` file contains the unit and integration test suite for the `TaskSubmit` component. It verifies the functionality of the task submission interface, including executor type selection, JSON validation logic, and form state management.

## Public API

### `wrapper`
A React component wrapper used to provide the necessary context providers for testing the `TaskSubmit` component.

*   **Signature**: `function wrapper({ children }: { children: React.ReactNode })`
*   **Purpose**: Supplies a `QueryClientProvider` (with `retry: false` for deterministic testing) and a `MemoryRouter` to simulate the application's routing and data-fetching environment.

## Dependencies

The test suite relies on the following libraries and internal modules:

*   **Testing Utilities**: `@testing-library/react` (rendering and DOM interaction), `vitest` (test runner and mocking).
*   **State Management**: `@tanstack/react-query` for handling asynchronous API state.
*   **Routing**: `react-router-dom` for navigation context.
*   **Mocked Modules**:
    *   `@/api/tasks`: Mocks the `useSubmitTask` hook.
    *   `@/hooks/use-auth`: Mocks authentication state and providers.
    *   `@/hooks/use-alerts`: Mocks alert notification systems.
    *   `sonner`: Mocks toast notification UI.

## Usage Notes

*   **Mocking Strategy**: The test suite uses `vi.mock` to isolate the `TaskSubmit` component from external API calls and global providers. Authentication and alert hooks are mocked to return static, predictable values.
*   **Environment**: The tests utilize `jsdom` via `vitest`. The `beforeEach` block ensures that `localStorage` is cleared before every test to prevent cross-test state pollution.
*   **Test Coverage**:
    *   **UI Rendering**: Verifies the presence of executor type buttons (SIMULATED, SHELL, DOCKER, PYTHON).
    *   **Validation**: Confirms that the JSON validation logic correctly identifies valid vs. invalid input.
    *   **Interaction**: Ensures that clicking an executor type button updates the textarea content with the corresponding template.
*   **Execution**: Run these tests using the project's standard test command (e.g., `npm test` or `vitest`). Ensure all mocked dependencies match the interface of the actual production hooks to avoid false negatives.
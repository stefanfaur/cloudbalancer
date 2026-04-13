# File: web-dashboard/src/__tests__/role-visibility.test.tsx

## Overview

`web-dashboard/src/__tests__/role-visibility.test.tsx` is an integration test suite designed to verify role-based access control (RBAC) within the web dashboard. It ensures that UI elements, specifically navigation links, are conditionally rendered based on the authenticated user's permissions.

The test suite validates that users with `ADMIN` privileges can access restricted areas (e.g., "Settings"), while users with `VIEWER` privileges are restricted from seeing those same elements.

## Public API

This file is a test suite and does not export any public APIs. It provides the following internal helper function:

*   **`loginAs(username: string, password: string)`**: An asynchronous helper function that automates the authentication flow. It renders the `App` component, interacts with the login form via `userEvent`, and waits for the "Cluster Overview" page to load, confirming a successful login.

## Dependencies

This test file relies on the following internal and external modules:

*   **`@testing-library/react`**: Used for rendering the `App` component and querying the DOM.
*   **`@testing-library/user-event`**: Used to simulate user interactions (typing, clicking).
*   **`vitest`**: The test runner and assertion library.
*   **`@/test-mocks/server`**: A Mock Service Worker (MSW) server instance used to intercept network requests and simulate backend authentication responses.
*   **`@/App`**: The root component of the application being tested.

## Usage Notes

*   **Mocking**: The test suite uses MSW to manage network requests. The `server` is configured to `listen` before all tests, `resetHandlers` after each test, and `close` after all tests to ensure test isolation.
*   **Timeouts**: The `loginAs` helper includes a 5000ms timeout for the `waitFor` assertion to account for potential latency in the authentication flow.
*   **Execution**: These tests are intended to be run via the Vitest CLI. Ensure that the MSW handlers in `@/test-mocks/server` are correctly configured to handle the credentials provided in the `loginAs` calls ("admin" and "viewer").
*   **Maintenance**: If the login form UI changes (e.g., placeholder text or button labels), the `loginAs` helper function must be updated to maintain test compatibility.
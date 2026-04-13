# File: web-dashboard/src/__tests__/auth-flow.test.tsx

## Overview

The `web-dashboard/src/__tests__/auth-flow.test.tsx` file contains integration tests for the application's authentication lifecycle. It verifies the end-to-end behavior of the login process, including redirection for unauthenticated users, successful authentication flows, and error handling for invalid credentials.

The suite utilizes `vitest` as the test runner and `msw` (via the `server` mock) to intercept network requests, ensuring that the authentication logic is tested against predictable API responses without requiring a live backend.

## Public API

This file is a test suite and does not export any functions or components for use in the application. It interacts with the following internal components:

*   **`App`**: The root component of the application, used as the entry point for mounting the test environment.
*   **`server`**: The Mock Service Worker (MSW) instance used to mock API endpoints for authentication.

## Dependencies

*   **`@testing-library/react`**: Provides utilities for rendering components and querying the DOM.
*   **`@testing-library/user-event`**: Simulates realistic user interactions (typing, clicking).
*   **`vitest`**: The testing framework providing the `describe`, `it`, and `expect` primitives.
*   **`@/test-mocks/server`**: The MSW server configuration used to mock backend responses.
*   **`@/App`**: The main application component under test.

## Usage Notes

*   **Test Environment**: These tests rely on `msw` to intercept network requests. Ensure that the `server` mock is correctly configured to handle the authentication endpoints (`/login` or equivalent) used by the `App`.
*   **Asynchronous Handling**: Because authentication involves network requests, all tests use `await waitFor` to handle the asynchronous nature of state updates and DOM rendering.
*   **Timeouts**: Some tests include explicit `timeout` options in `waitFor` calls (e.g., 5000ms for successful login) to account for potential latency in the mock server or component re-renders.
*   **Lifecycle Hooks**: The suite uses `beforeAll`, `afterEach`, and `afterAll` to manage the lifecycle of the mock server, ensuring that handlers are reset between tests to prevent state leakage.
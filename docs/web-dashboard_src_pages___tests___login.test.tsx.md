# File: web-dashboard/src/pages/__tests__/login.test.tsx

## Overview

`web-dashboard/src/pages/__tests__/login.test.tsx` is a Vitest-based integration test suite for the `LoginPage` component. It validates the authentication flow, including form rendering, error handling for invalid credentials, and UI state management during asynchronous login requests. The suite utilizes Mock Service Worker (MSW) to intercept network requests and React Testing Library to simulate user interactions.

## Public API

### `fakeJwt(sub: string, role: string): string`
Generates a mock JSON Web Token (JWT) string for testing purposes.
- **Parameters**:
  - `sub`: The subject (user identifier) to encode in the payload.
  - `role`: The user role to encode in the payload.
- **Returns**: A string formatted as `header.payload.signature` using Base64 encoding.

## Dependencies

- **Testing Frameworks**: `vitest`, `@testing-library/react`, `@testing-library/user-event`.
- **Network Mocking**: `msw` (Mock Service Worker) for intercepting API calls to the authentication endpoint.
- **State Management**: `@tanstack/react-query` for handling server state and caching.
- **Routing**: `react-router-dom` for providing the routing context required by the `LoginPage`.
- **Internal Hooks**: `@/hooks/use-auth` for the `AuthProvider` context.
- **Components**: `@/pages/login` (the component under test).

## Usage Notes

- **Test Environment**: The suite uses a global `setupServer` instance from MSW. The server is configured to listen before all tests, reset handlers after each test, and close after the suite completes to ensure test isolation.
- **Rendering**: The `renderLogin` helper function wraps the `LoginPage` in the necessary providers (`QueryClientProvider`, `BrowserRouter`, and `AuthProvider`) to simulate the production environment.
- **Mocking Strategy**: 
  - To test error states (e.g., 401 Unauthorized), use `server.use()` to override the default login handler with a specific `HttpResponse` status.
  - To test loading states, use `server.use()` with an asynchronous handler that includes a `setTimeout` delay, allowing the test to verify that the submit button is disabled during the pending state.
- **JWT Generation**: The `fakeJwt` function is used to simulate successful authentication responses, providing a valid-looking token structure for the authentication hooks to process.
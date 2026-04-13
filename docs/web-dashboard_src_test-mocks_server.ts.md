# File: web-dashboard/src/test-mocks/server.ts

## Overview

The `web-dashboard/src/test-mocks/server.ts` file serves as the central configuration point for Mock Service Worker (MSW) in the testing environment. It initializes a network request interception server that allows the application to mock API responses during unit and integration tests, ensuring that tests remain deterministic and isolated from actual backend services.

## Public API

### `server`
- **Type**: `SetupServerApi`
- **Description**: An instance of the MSW server configured with the application's request handlers. This object provides methods to control the lifecycle of the mock server, such as `listen()`, `close()`, and `resetHandlers()`.

## Dependencies

### Internal Imports
- `web-dashboard/src/test-mocks/handlers.ts`: Imports the collection of request handlers that define how the mock server should respond to specific network requests.

### External Imports
- `msw/node`: Provides the `setupServer` function used to create the mock server instance for Node.js-based test environments (e.g., Vitest or Jest).

## Usage Notes

- **Integration**: This server instance is typically used in test setup files (e.g., `setupTests.ts`) to ensure the mock server starts before tests run and shuts down after they complete.
- **Lifecycle Management**:
    - Call `server.listen()` in the `beforeAll` hook of your test setup.
    - Call `server.resetHandlers()` in the `afterEach` hook to prevent state leakage between tests.
    - Call `server.close()` in the `afterAll` hook to clean up the server instance.
- **Extensibility**: To add new mock endpoints, update the `handlers.ts` file. The `server` instance will automatically incorporate these changes as it imports the `handlers` array.
- **Testing Context**: This file is intended for use in test suites only (e.g., `auth-flow.test.tsx`, `role-visibility.test.tsx`) and should not be included in the production application bundle.
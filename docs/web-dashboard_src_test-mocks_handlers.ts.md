# File: web-dashboard/src/test-mocks/handlers.ts

## Overview

`web-dashboard/src/test-mocks/handlers.ts` defines the network request interception layer for the application's test suite using [Mock Service Worker (MSW)](https://mswjs.io/). It provides a comprehensive mock implementation of the backend API and metrics services, allowing frontend components to be tested in isolation without requiring a live backend.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it defines the contract between the frontend and the backend, any changes here can lead to false positives or negatives in the test suite. Ensure that any modifications to the API structure in the real backend are mirrored here immediately.

## Public API

The module exports a single constant `handlers`, which is an array of MSW `http` request handlers.

### Exported Members
*   `handlers`: An array of `HttpHandler` objects that intercept requests to `http://localhost:8080` (API) and `http://localhost:8081` (Metrics).

### Mocked Endpoints
*   **Authentication**:
    *   `POST /api/auth/login`: Validates credentials. Supports `admin/admin` and `viewer/viewer` to return specific JWTs.
    *   `POST /api/auth/refresh`: Returns a mock refresh token.
    *   `POST /api/auth/logout`: Returns a `204 No Content` status.
*   **Task Management**:
    *   `GET /api/tasks`: Returns a paginated list of mock tasks.
    *   `POST /api/tasks`: Returns a newly created task object.
*   **Metrics**:
    *   `GET /api/metrics/cluster`: Returns aggregated cluster health data.
    *   `GET /api/metrics/workers`: Returns a list of worker snapshots.
*   **System Configuration**:
    *   `GET /api/scaling/status`: Returns current auto-scaling configuration.
    *   `GET/PUT /api/admin/strategy`: Manages the task distribution strategy.

## Dependencies

*   `msw`: The core library used for network request interception.

## Usage Notes

### Integrating with Tests
This file is intended to be imported by `web-dashboard/src/test-mocks/server.ts` to initialize the MSW server instance.

**Example: Setting up the test server**
```typescript
// web-dashboard/src/test-mocks/server.ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
```

### Testing Scenarios
1.  **Authentication States**: Use the `admin` or `viewer` credentials to test role-based UI rendering.
2.  **Dynamic Data**: The handlers use `new Date().toISOString()` for timestamps. If your tests rely on specific time-based logic, consider overriding these handlers in specific test files using `server.use()`.
3.  **Error Handling**: To test how the UI handles API failures, you can override specific handlers in your test file:
    ```typescript
    import { server } from '../test-mocks/server';
    import { http, HttpResponse } from 'msw';

    test('handles login failure', async () => {
      server.use(
        http.post('http://localhost:8080/api/auth/login', () => {
          return new HttpResponse(null, { status: 401 });
        })
      );
      // ... perform test
    });
    ```

### Potential Pitfalls
*   **Hardcoded URLs**: The handlers use hardcoded base URLs (`http://localhost:8080`). If the application environment variables change, these mocks may stop intercepting requests correctly.
*   **State Synchronization**: These mocks are stateless. If a test performs a `PUT` request to change the strategy, subsequent `GET` requests will still return the original mock data unless you implement a local state variable within the handler scope.
*   **JWT Validity**: The tokens (`ADMIN_TOKEN`, `VIEWER_TOKEN`) are static strings. They are not cryptographically valid, so any backend-side validation logic will fail if the frontend attempts to send these to a real server.
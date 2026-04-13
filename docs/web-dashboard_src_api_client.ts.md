# File: web-dashboard/src/api/client.ts

## Overview

The `web-dashboard/src/api/client.ts` module serves as the core networking layer for the web dashboard. It provides a centralized, authenticated fetch wrapper (`apiFetch`) that handles environment-specific base URLs, automatic authorization header injection, and token refresh logic.

This module is designed to be decoupled from the authentication implementation, allowing the application to inject authentication logic at runtime via `setAuthFunctions`.

## Public API

### Types
*   **`TokenGetter`**: A function type `() => string | null` used to retrieve the current authentication token.
*   **`RefreshFn`**: A function type `() => Promise<boolean>` used to attempt a token refresh.

### Functions
*   **`setAuthFunctions(getter: TokenGetter, refresh: RefreshFn)`**: Configures the authentication lifecycle hooks. This must be called during application initialization (typically in an `AuthProvider`).
*   **`apiFetch<T>(path: string, options?: RequestInit): Promise<T>`**: A wrapper around the native `fetch` API. It automatically selects the base URL (API vs. Metrics), attaches `Authorization` headers, handles 401 unauthorized responses by triggering a refresh, and parses JSON responses.

### Classes
*   **`ApiError`**: A custom error class extending `Error` that captures HTTP status codes and response bodies for better debugging and error handling in UI components.
    *   **`constructor(status: number, statusText: string, body: string)`**: Initializes the error with the response details.

## Dependencies

This module is a low-level utility and does not import other internal modules. It relies on:
*   **Environment Variables**: `VITE_API_URL` and `VITE_METRICS_URL` for endpoint resolution.
*   **Global `fetch` API**: Used for all network requests.

## Usage Notes

### Initialization
Before making any API calls, the application must register the authentication handlers. This is typically done in the root component or an authentication provider:

```typescript
import { setAuthFunctions } from '@/api/client';

setAuthFunctions(
  () => localStorage.getItem('token'),
  async () => {
    // Logic to refresh token
    return true;
  }
);
```

### Making Requests
The `apiFetch` function automatically routes requests to the correct base URL based on the path prefix. Paths starting with `/api/metrics/` are routed to `VITE_METRICS_URL`, while all others are routed to `VITE_API_URL`.

```typescript
try {
  const data = await apiFetch<User[]>('/api/users');
} catch (err) {
  if (err instanceof ApiError) {
    console.error(`Request failed with status ${err.status}: ${err.body}`);
  }
}
```

### Error Handling
The client throws an `ApiError` if the response status is not "ok" (i.e., outside the 200-299 range). Always wrap `apiFetch` calls in `try/catch` blocks to handle network failures or server-side errors gracefully.
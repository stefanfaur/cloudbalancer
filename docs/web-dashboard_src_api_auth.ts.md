# File: web-dashboard/src/api/auth.ts

## Overview

The `web-dashboard/src/api/auth.ts` module serves as the primary authentication interface for the web dashboard. It provides asynchronous functions to manage user sessions, including logging in, refreshing authentication tokens, and logging out. The module interacts directly with the backend API defined by the `VITE_API_URL` environment variable.

## Public API

### `login(username: string, password: string): Promise<AuthResponse>`
Authenticates a user with the provided credentials.
- **Parameters**: 
    - `username`: The user's identifier.
    - `password`: The user's secret password.
- **Returns**: A promise that resolves to an `AuthResponse` object containing authentication tokens.
- **Throws**: An error if the response status is not OK (e.g., invalid credentials).

### `refresh(refreshToken: string): Promise<AuthResponse>`
Requests a new set of authentication tokens using a valid refresh token.
- **Parameters**: 
    - `refreshToken`: The current refresh token string.
- **Returns**: A promise that resolves to a new `AuthResponse`.
- **Throws**: An error if the refresh request fails.

### `logout(accessToken: string): Promise<void>`
Invalidates the current session on the server.
- **Parameters**: 
    - `accessToken`: The active access token for the session to be terminated.
- **Returns**: A promise that resolves when the logout request is complete.

## Dependencies

- `web-dashboard/src/api/types.ts`: Provides the `AuthResponse` type definition used for return values.
- `import.meta.env`: Used to retrieve the `VITE_API_URL` for dynamic endpoint construction.

## Usage Notes

- **Environment Configuration**: This module relies on `VITE_API_URL`. Ensure this environment variable is correctly set in your `.env` file; it defaults to `http://localhost` if not provided.
- **Integration**: This module is primarily consumed by `web-dashboard/src/hooks/use-auth.tsx` within the `AuthProvider`. It is recommended to use the provided hooks rather than calling these API functions directly in components to ensure consistent state management.
- **Error Handling**: All functions perform basic validation on the `response.ok` property. Consumers should implement appropriate `try/catch` blocks to handle network failures or authentication errors gracefully.
- **Authorization**: The `logout` function requires an `accessToken` to be passed in the `Authorization` header, adhering to standard Bearer token authentication patterns.
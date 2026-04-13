# File: web-dashboard/src/hooks/use-auth.tsx

## Overview

The `web-dashboard/src/hooks/use-auth.tsx` file is a critical component of the application's security architecture. It provides a React Context-based authentication state management system, handling login/logout flows, JWT parsing, and automatic token refreshing.

**Warning: This file is a HOTSPOT.** It ranks in the top 25% for both change frequency and complexity. As the central authority for user sessions, any modifications to this file carry a high risk of introducing regressions in authentication or session persistence.

## Public API

### `AuthState` (Interface)
Defines the shape of the authentication context:
- `user`: The username or subject identifier.
- `role`: The assigned `Role` of the user.
- `accessToken`: The current JWT string.
- `isAuthenticated`: Boolean flag indicating active session status.
- `isLoading`: Boolean flag indicating if the initial silent refresh is in progress.
- `login`: Async function `(u, p) => Promise<void>`.
- `logout`: Async function `() => Promise<void>`.

### `AuthProvider` (Component)
A React provider component that wraps the application (or a sub-tree) to inject authentication state. It manages the lifecycle of the `accessToken`, including scheduling background refreshes.

### `useAuth` (Hook)
The primary hook for consuming authentication state.
- **Throws**: Error if called outside of an `AuthProvider`.

### `parseJwt` (Function)
Utility to decode the JWT payload without verification. Returns the `sub`, `role`, and `exp` claims.

### `getItem`, `setItem`, `removeItem`
Internal wrappers for `localStorage` that provide safe access by catching potential exceptions (e.g., when cookies/storage are disabled by browser privacy settings).

## Dependencies

- **`@/api/auth`**: Provides the underlying `login`, `logout`, and `refresh` network calls.
- **`@/api/client`**: Used to inject the `accessToken` and `doRefresh` logic into the global API client, ensuring all outgoing requests are authenticated.
- **`@/api/types`**: Provides the `Role` type definition.

## Usage Notes

### Implementation Rationale
The `AuthProvider` implements a silent refresh strategy. Upon mounting, it checks for a `cb-refresh-token` in `localStorage`. If found, it attempts to refresh the session. It also schedules a timer to refresh the `accessToken` at 80% of its TTL (Time-to-Live) to ensure the user session remains uninterrupted.

### Common Pitfalls
1. **Race Conditions**: Because `doRefresh` is called both on mount and via a timer, ensure that `refreshTimerRef` is properly cleared to prevent multiple concurrent refresh requests.
2. **Storage Failures**: The `safeStorage` wrapper prevents the app from crashing if `localStorage` is inaccessible, but it will result in an unauthenticated state.
3. **Dependency Cycles**: Be cautious when adding dependencies to `useEffect` hooks within this file, as they trigger re-initialization of the API client's auth functions.

### Example Usage

```tsx
// Wrapping the application
function App() {
  return (
    <AuthProvider>
      <Dashboard />
    </AuthProvider>
  );
}

// Consuming in a component
function UserProfile() {
  const { user, role, logout } = useAuth();

  if (!user) return <Redirect to="/login" />;

  return (
    <div>
      <p>Logged in as: {user} ({role})</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

### Troubleshooting
- **Infinite Refresh Loops**: If `doRefresh` fails, it calls `clearAuth()`. Ensure the backend returns a 4xx status code for expired/invalid refresh tokens to trigger this cleanup correctly.
- **Missing Auth**: If `useAuth` throws an error, ensure the component is rendered as a descendant of `AuthProvider`.
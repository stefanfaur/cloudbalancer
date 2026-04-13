# File: web-dashboard/src/App.tsx

## Overview

`web-dashboard/src/App.tsx` serves as the primary entry point and central routing configuration for the web dashboard application. It orchestrates the application's global state, authentication flow, and code-splitting strategy.

**Note: This file is a HOTSPOT.** It is in the top 25% for both change frequency and complexity. As the central hub for routing and provider wrapping, modifications here carry a high risk of breaking the entire application. Exercise extreme caution when altering route definitions or provider nesting.

## Public API

### `LoadingFallback()`
A functional component that renders a skeleton-based loading state. It is used as the `fallback` prop for `React.Suspense` to provide visual feedback while lazy-loaded pages are being fetched.

### `ProtectedRoute({ children })`
A wrapper component that enforces authentication.
- **Logic**: It consumes the `useAuth` hook to check the `isAuthenticated` status.
- **Behavior**: 
    - If `isLoading` is true, it displays a centered loading indicator.
    - If unauthenticated, it redirects the user to the `/login` route.
    - If authenticated, it renders the provided `children`.

### `AppRoutes()`
Defines the application's routing table using `react-router-dom`. It manages both public routes (like `/login`) and protected routes nested within the `DashboardLayout`.

## Dependencies

This file integrates several core infrastructure services:

- **State Management**: `@tanstack/react-query` for server-state caching and synchronization.
- **Authentication**: `AuthProvider` and `useAuth` from `@/hooks/use-auth`.
- **Notifications**: `AlertsProvider` from `@/hooks/use-alerts`.
- **Routing**: `react-router-dom` for navigation and route protection.
- **UI Components**: `Skeleton` for loading states.

## Usage Notes

### Code Splitting
The application utilizes `React.lazy` for route-based code splitting. When adding new pages, ensure they are imported via `lazy` to keep the initial bundle size small.

### Adding New Routes
To add a new page to the dashboard:
1. Create the page component in `src/pages/`.
2. Import it lazily in `App.tsx`:
   ```typescript
   const NewPage = lazy(() => import("@/pages/new-page"));
   ```
3. Add the route inside the `DashboardLayout` route block in `AppRoutes`:
   ```typescript
   <Route path="/new-path" element={<NewPage />} />
   ```

### Provider Nesting
The application uses a specific provider hierarchy. If you need to add a new global context provider, ensure it is placed within the `App` component's tree:
```typescript
<QueryClientProvider client={queryClient}>
  <BrowserRouter>
    <AuthProvider>
      <AlertsProvider>
        {/* New Providers go here */}
        <Suspense fallback={<LoadingFallback />}>
          <AppRoutes />
        </Suspense>
      </AlertsProvider>
    </AuthProvider>
  </BrowserRouter>
</QueryClientProvider>
```

### Troubleshooting
- **Routing Loops**: If users are stuck in a redirect loop, verify the `ProtectedRoute` logic and ensure the `/login` route is not wrapped in `ProtectedRoute`.
- **Loading States**: If a page fails to load, check the `Suspense` boundary and ensure the lazy-loaded component is correctly exported from its file.
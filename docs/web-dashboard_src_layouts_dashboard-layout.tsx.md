# File: web-dashboard/src/layouts/dashboard-layout.tsx

## Overview

The `DashboardLayout` component serves as the primary structural shell for the application's authenticated interface. It implements a responsive sidebar-navigation pattern, providing a consistent layout across all administrative modules (Cluster, Tasks, Workers, Agents, Analytics, and Settings).

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As the primary entry point for the authenticated dashboard, any regressions here will impact the entire user interface. Exercise extreme caution when modifying navigation logic or authentication state handling.

## Public API

### `handleLogout()`
An asynchronous function that triggers the authentication logout flow.
- **Behavior**: Calls the `logout` method from `useAuth`, then redirects the user to the `/login` route via `react-router-dom`'s `useNavigate`.
- **Usage**: Invoked via the logout button in the top header.

## Dependencies

This component relies on several core architectural hooks and UI components:

*   **Authentication**: `useAuth` (manages user session and role state).
*   **Real-time**: `useWebSocket` (monitors connection health for the `ConnectionIndicator`).
*   **Routing**: `react-router-dom` (`Outlet` for nested route rendering, `NavLink` for navigation).
*   **UI Components**: `ConnectionIndicator`, `AlertsBanner`, `Badge`, `Button`.
*   **Icons**: `lucide-react` (standardized iconography for navigation).

## Usage Notes

### Layout Structure
The layout is divided into three main sections:
1.  **Sidebar (`aside`)**: Contains the application branding, navigation links defined in `NAV_ITEMS`, and the `ConnectionIndicator`. It uses responsive classes to toggle between a collapsed state (`lg`) and an expanded state (`xl`).
2.  **Header (`header`)**: Displays the current user identity, their role badge, and the logout trigger.
3.  **Main Content (`main`)**: Wraps the `AlertsBanner` and the `Outlet`, which renders the child routes defined in the application router.

### Implementation Rationale
*   **Responsive Design**: The sidebar uses `shrink-0` and specific width utilities (`w-56`, `lg:w-14`) to ensure the dashboard remains functional on smaller screens while maintaining a desktop-first design.
*   **State Management**: By wrapping the `Outlet` within this layout, we ensure that global UI elements (like the `AlertsBanner`) are always present regardless of the specific dashboard sub-page being viewed.

### Potential Pitfalls
*   **Route Protection**: This layout assumes the user is already authenticated. Ensure that the parent route in `App.tsx` is wrapped in an authentication guard; otherwise, the `useAuth` hook may return null values, potentially causing runtime errors in the header.
*   **WebSocket Reconnection**: The `ConnectionIndicator` relies on `useWebSocket`. If the WebSocket provider is not initialized higher in the component tree, this layout will fail to display connection status correctly.
*   **Navigation Updates**: When adding new modules, ensure they are added to the `NAV_ITEMS` constant. Failure to do so will result in a broken navigation experience, as the sidebar is dynamically generated from this array.

### Example: Adding a New Navigation Item
To add a new section to the dashboard, update the `NAV_ITEMS` array:

```typescript
const NAV_ITEMS = [
  // ... existing items
  { to: "/logs", label: "System Logs", icon: FileText },
] as const;
```
The `NavLink` component will automatically handle the `isActive` styling and rendering based on the route path.
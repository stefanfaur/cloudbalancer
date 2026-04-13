# File: web-dashboard/src/components/connection-indicator.tsx

## Overview

The `ConnectionIndicator` component provides a visual status indicator for the application's network or service connectivity. It displays a color-coded status dot accompanied by a text label, allowing users to quickly identify if the system is live, currently reconnecting, or disconnected.

## Public API

### `ConnectionIndicatorProps`

| Property | Type | Description |
| :--- | :--- | :--- |
| `isConnected` | `boolean` | Indicates if the connection is currently active. |
| `isReconnecting` | `boolean` | Indicates if the system is currently attempting to re-establish a connection. |

### `ConnectionIndicator`

A functional React component that renders the status UI.

**Visual Logic:**
- **Live**: Green dot (`bg-emerald-500`) when `isConnected` is true.
- **Reconnecting**: Amber pulsing dot (`bg-amber-500 animate-pulse`) when `isReconnecting` is true.
- **Disconnected**: Red dot (`bg-red-500`) when neither `isConnected` nor `isReconnecting` are true.

## Dependencies

- `web-dashboard/src/lib/utils.ts`: Provides the `cn` utility for conditional Tailwind CSS class merging.

## Usage Notes

- The component is designed to be lightweight and is typically placed in headers or status bars within the dashboard layout.
- Ensure that the state passed to `isConnected` and `isReconnecting` is mutually exclusive where appropriate to avoid conflicting visual states, although the component logic prioritizes `isConnected` and `isReconnecting` over the disconnected state.
- This component is utilized by `web-dashboard/src/layouts/dashboard-layout.tsx` to provide global connectivity feedback to the user.
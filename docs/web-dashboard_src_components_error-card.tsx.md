# File: web-dashboard/src/components/error-card.tsx

## Overview

The `ErrorCard` component is a standardized UI element used throughout the web dashboard to gracefully handle and display error states. It provides a consistent visual feedback mechanism when data fetching or component rendering fails, featuring a prominent warning icon, the error message, and a retry action.

## Public API

### `ErrorCardProps`
Interface defining the properties required by the `ErrorCard` component.

| Property | Type | Description |
| :--- | :--- | :--- |
| `error` | `Error` | The error object containing the message to be displayed. |
| `onRetry` | `() => void` | A callback function triggered when the user clicks the "Retry" button. |

### `ErrorCard`
A functional React component that renders an error message within a styled card container.

```typescript
function ErrorCard({ error, onRetry }: ErrorCardProps)
```

## Dependencies

The component relies on the following internal UI components and external libraries:

*   **`@/components/ui/card`**: Provides the `Card` and `CardContent` layout wrappers.
*   **`@/components/ui/button`**: Provides the `Button` component for the retry action.
*   **`lucide-react`**: Provides the `AlertTriangle` icon used for visual emphasis.

## Usage Notes

*   **Consistency**: This component is intended to be used as a fallback UI in pages and data-fetching components to ensure a unified user experience during failures.
*   **Styling**: The component uses a dark theme (`bg-slate-900`) with red accents (`border-red-900/30`, `text-red-400`) to clearly communicate an error state to the user.
*   **Implementation**: When implementing `onRetry`, ensure the parent component resets the relevant state (e.g., clearing error states or re-triggering a `useEffect` hook) to allow the data fetching process to restart.
*   **Common Usage**: This component is widely utilized across the dashboard, including in `analytics.tsx`, `agents.tsx`, `cluster-overview.tsx`, and various task/worker list views.
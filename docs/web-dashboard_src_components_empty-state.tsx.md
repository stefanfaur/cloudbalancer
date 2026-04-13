# File: web-dashboard/src/components/empty-state.tsx

## Overview

The `EmptyState` component is a standardized UI element used throughout the web dashboard to provide visual feedback when a data-driven view contains no items. It displays a centered icon, a descriptive message, and an optional call-to-action link to guide users toward resolving the empty state (e.g., creating a new resource).

## Public API

### `EmptyStateProps`

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `icon` | `LucideIcon` | Yes | The icon component from `lucide-react` to display above the message. |
| `message` | `string` | Yes | The text to display explaining the empty state. |
| `actionLabel` | `string` | No | The label text for the optional action link. |
| `actionHref` | `string` | No | The destination URL for the optional action link. |

### `EmptyState` Component

A functional React component that renders the empty state UI. It conditionally renders an action link only if both `actionLabel` and `actionHref` are provided.

## Dependencies

- `react-router-dom`: Used for the `Link` component to handle internal navigation.
- `lucide-react`: Used for the `LucideIcon` type definition and icon rendering.

## Usage Notes

- **Conditional Rendering**: The action link is only rendered if both `actionLabel` and `actionHref` props are provided. If either is missing, the link will not appear.
- **Styling**: The component uses Tailwind CSS for layout and styling, centering content with `text-center` and providing vertical padding.
- **Integration**: This component is currently utilized in the `analytics` and `task-list` pages to handle scenarios where no data is available to display.

### Example Usage

```tsx
import { EmptyState } from "@/components/empty-state";
import { Inbox } from "lucide-react";

function MyComponent() {
  return (
    <EmptyState 
      icon={Inbox} 
      message="No tasks found." 
      actionLabel="Create a new task" 
      actionHref="/tasks/new" 
    />
  );
}
```
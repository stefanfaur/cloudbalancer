# File: web-dashboard/src/components/status-badge.tsx

## Overview

The `StatusBadge` component is a specialized UI element designed to display the lifecycle state of a task. It renders a `Badge` component containing a status-specific color-coded dot and the text representation of the current `TaskState`. This component ensures consistent visual representation of task statuses across the dashboard.

## Public API

### `StatusBadgeProps`

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `state` | `TaskState` | Yes | The current lifecycle state of the task. |
| `className` | `string` | No | Additional CSS classes to apply to the badge container. |

### `StatusBadge`

A functional React component that maps a `TaskState` to a pre-defined color configuration (text color and dot color) and renders it within an outlined badge.

## Dependencies

- **`@/components/ui/badge`**: Provides the base `Badge` component.
- **`@/lib/utils`**: Provides the `cn` utility for merging Tailwind CSS classes.
- **`@/api/types`**: Provides the `TaskState` union type definition.

## Usage Notes

- **State Mapping**: The component uses an internal `STATE_CONFIG` object to map every possible `TaskState` to specific Tailwind CSS classes. This ensures that statuses like `RUNNING` (blue) or `FAILED` (red) are visually distinct and consistent.
- **Styling**: The component is hardcoded with `font-mono` and `text-xs` to maintain a technical, clean aesthetic suitable for status indicators.
- **Integration**: This component is widely used across task and worker detail pages to provide immediate visual feedback on entity status.

### Example

```tsx
import { StatusBadge } from "@/components/status-badge";

function TaskRow({ task }) {
  return (
    <div>
      <span>{task.name}</span>
      <StatusBadge state={task.state} />
    </div>
  );
}
```
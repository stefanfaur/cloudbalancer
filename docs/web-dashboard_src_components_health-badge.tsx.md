# File: web-dashboard/src/components/health-badge.tsx

## Overview

The `HealthBadge` component is a specialized UI element designed to visualize the operational status of worker nodes within the system. It maps a `WorkerHealthState` to a color-coded badge, providing immediate visual feedback on the health of a worker (e.g., `HEALTHY`, `DEAD`, `DRAINING`).

The component utilizes a centralized `STATE_CONFIG` to ensure consistent styling across the application, pairing specific status labels with corresponding color schemes and indicator dots.

## Public API

### `HealthBadgeProps`

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `state` | `WorkerHealthState` | Yes | The current health status of the worker. |
| `className` | `string` | No | Additional CSS classes to override or extend the badge styling. |

### `HealthBadge`

A functional React component that renders a `Badge` containing a status indicator dot and the text representation of the provided `WorkerHealthState`.

```tsx
function HealthBadge({ state, className }: HealthBadgeProps)
```

## Dependencies

- `@/components/ui/badge`: Provides the base `Badge` component used for layout and styling.
- `@/lib/utils`: Provides the `cn` utility for merging Tailwind CSS classes.
- `@/api/types`: Imports the `WorkerHealthState` union type to ensure type safety for status values.

## Usage Notes

- **State Mapping**: The component automatically handles styling for the following states: `HEALTHY`, `SUSPECT`, `DEAD`, `DRAINING`, `RECOVERING`, and `STOPPING`.
- **Styling**: The component uses `font-mono` for the text and a small indicator dot (`1.5w x 1.5h`) to maintain a clean, dashboard-appropriate aesthetic.
- **Integration**: This component is widely used across the dashboard, including in `cluster-overview.tsx`, `worker-list.tsx`, and `worker-detail.tsx` to maintain a unified visual language for worker health.
- **Customization**: You can pass additional Tailwind classes via the `className` prop to adjust margins or padding if the badge is used in tight layouts.
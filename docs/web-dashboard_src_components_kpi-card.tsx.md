# File: web-dashboard/src/components/kpi-card.tsx

## Overview

The `KpiCard` component is a specialized UI element designed to display Key Performance Indicators (KPIs) within the web dashboard. It provides a clean, consistent layout for presenting a numerical value alongside a descriptive label and an optional trend indicator (up, down, or neutral).

The component is built using the project's standardized `Card` primitive and is optimized for dashboard layouts where multiple metrics need to be compared at a glance.

## Public API

### `KpiCardProps`

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `label` | `string` | Yes | The descriptive text displayed below the KPI value. |
| `value` | `string \| number` | Yes | The primary metric value to display. |
| `icon` | `LucideIcon` | Yes | The icon component from `lucide-react` to display on the left. |
| `trend` | `"up" \| "down" \| "neutral"` | No | Optional trend indicator that changes the icon and color. |
| `className` | `string` | No | Additional CSS classes to apply to the card container. |

## Dependencies

*   **`@/components/ui/card`**: Provides the base `Card` and `CardContent` layout components.
*   **`@/lib/utils`**: Provides the `cn` utility for conditional class name merging.
*   **`lucide-react`**: Provides the icon library for the KPI icon and trend indicators.

## Usage Notes

*   **Styling**: The component defaults to a dark theme (`bg-slate-900`, `border-slate-700`) consistent with the dashboard's aesthetic. Use the `className` prop to override or extend these styles.
*   **Trend Indicators**: If the `trend` prop is provided, the component automatically renders the corresponding icon (`TrendingUp`, `TrendingDown`, or `Minus`) and applies color-coded text classes (`text-emerald-400`, `text-red-400`, or `text-slate-500`).
*   **Layout**: The component is designed to be responsive and uses `tabular-nums` for the value to ensure consistent alignment when values change dynamically.

### Example Usage

```tsx
import { Activity } from "lucide-react";
import { KpiCard } from "@/components/kpi-card";

function Dashboard() {
  return (
    <KpiCard 
      label="Active Nodes" 
      value={12} 
      icon={Activity} 
      trend="up" 
    />
  );
}
```
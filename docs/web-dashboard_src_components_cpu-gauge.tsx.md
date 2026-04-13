# File: web-dashboard/src/components/cpu-gauge.tsx

## Overview

The `CpuGauge` component is a reusable UI element designed to visualize CPU utilization percentages. It renders a circular progress gauge that dynamically updates its stroke color based on the provided percentage, providing immediate visual feedback on system load. It is commonly used across the dashboard to monitor worker and cluster health.

## Public API

### `CpuGaugeProps`
The interface defining the properties for the `CpuGauge` component:

| Property | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `percent` | `number` | Yes | The CPU utilization percentage (0-100). |
| `size` | `number` | No | The diameter of the gauge in pixels. Defaults to `48`. |
| `className` | `string` | No | Additional CSS classes to apply to the container. |

### `gaugeColor(pct: number)`
A utility function that determines the stroke color class based on the utilization percentage:
*   **>= 80%**: `stroke-red-500` (Critical)
*   **>= 60%**: `stroke-amber-500` (Warning)
*   **< 60%**: `stroke-emerald-500` (Healthy)

## Dependencies

*   `@/lib/utils`: Provides the `cn` utility for merging Tailwind CSS classes.

## Usage Notes

*   **Responsiveness**: The gauge uses a fixed size defined by the `size` prop, but the SVG container is wrapped in a `div` that supports external styling via the `className` prop.
*   **Animation**: The gauge includes a CSS transition (`transition-all duration-500`) to smoothly animate the progress bar when the `percent` value updates.
*   **Display**: The component renders the percentage as a centered, tabular-numbered text element, ensuring consistent alignment regardless of the number of digits.
*   **Integration**: This component is widely used in cluster and worker monitoring pages, such as `worker-list.tsx` and `worker-detail.tsx`, to provide a standardized view of resource consumption.
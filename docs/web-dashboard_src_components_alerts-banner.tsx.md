# File: web-dashboard/src/components/alerts-banner.tsx

## Overview

The `AlertsBanner` component is a UI element designed to display active system notifications or alerts within the dashboard. It integrates with the `useAlerts` hook to manage the state of alerts, providing users with visibility into system status, warnings, or errors. The component automatically handles severity-based styling and provides functionality to dismiss individual alerts or clear the entire queue.

## Public API

### `AlertsBanner`
A functional React component that renders the alert interface.

- **Props**: None.
- **Behavior**:
    - Automatically hides itself if there are no active alerts.
    - Limits the display to the 5 most recent alerts.
    - Displays a "Clear all" button if more than one alert is present.
    - Displays a summary count if more than 5 alerts are present.

## Dependencies

- **`@/hooks/use-alerts`**: Provides the `alerts` array, `dismiss` function, and `clearAll` function.
- **`lucide-react`**: Provides iconography (`AlertTriangle`, `Info`, `XCircle`, `X`).
- **`@/lib/utils`**: Provides the `cn` utility for conditional Tailwind class merging.

## Usage Notes

- **Severity Mapping**: The component uses a `SEVERITY_CONFIG` object to map alert levels (`error`, `warning`, `info`) to specific icons and Tailwind color schemes. If an unknown severity is provided, it defaults to the `info` configuration.
- **Layout Integration**: This component is intended to be placed at the top of the main content area or within a layout wrapper (e.g., `dashboard-layout.tsx`) to ensure visibility.
- **Performance**: The component renders a maximum of 5 alerts at a time. If the alert queue exceeds this, a counter is displayed to inform the user of hidden alerts.
- **Styling**: The component uses Tailwind CSS classes for styling. It expects a dark-mode-compatible environment given the `text-slate-200` and `text-slate-500` color choices.
# File: web-dashboard/src/hooks/use-alerts.tsx

## Overview

`web-dashboard/src/hooks/use-alerts.tsx` provides a centralized state management system for application-wide notifications and alerts. It implements the React Context API to allow any component within the application tree to trigger, dismiss, or clear system alerts. The hook enforces a maximum capacity of 50 active alerts to prevent memory bloat and UI clutter.

## Public API

### Interfaces

*   **`Alert`**: Defines the structure of a notification.
    *   `severity` (string): The urgency level of the alert.
    *   `message` (string): The display text for the alert.
    *   `timestamp` (string): The time the alert was generated.
*   **`AlertsState`**: The shape of the context value provided to the application.
    *   `alerts`: An array of `Alert` objects.
    *   `addAlert(alert: Alert)`: Adds a new alert to the top of the list.
    *   `dismiss(index: number)`: Removes a specific alert by its index.
    *   `clearAll()`: Removes all active alerts.

### Functions

*   **`AlertsProvider({ children })`**: A React provider component that must wrap the application (or a sub-tree) to enable alert functionality.
*   **`useAlerts()`**: A custom hook used to access the `AlertsState`. It throws an error if called outside of an `AlertsProvider`.

## Dependencies

*   **React**: Uses `createContext`, `useContext`, `useState`, `useCallback`, and `ReactNode` for state management and component wrapping.

## Usage Notes

### Implementation
To use the alert system, wrap your application root or the relevant component tree with the `AlertsProvider`:

```tsx
import { AlertsProvider } from "./hooks/use-alerts";

function App() {
  return (
    <AlertsProvider>
      <YourAppContent />
    </AlertsProvider>
  );
}
```

### Consuming Alerts
Within any child component, use the `useAlerts` hook to interact with the alert state:

```tsx
import { useAlerts } from "./hooks/use-alerts";

function NotificationComponent() {
  const { alerts, addAlert, dismiss } = useAlerts();

  return (
    <div>
      {alerts.map((alert, index) => (
        <div key={index} onClick={() => dismiss(index)}>
          {alert.message}
        </div>
      ))}
    </div>
  );
}
```

### Constraints
*   **Capacity**: The system automatically truncates the alert list to a maximum of 50 items using `MAX_ALERTS`.
*   **Context Requirement**: Attempting to call `useAlerts()` outside of an `AlertsProvider` will result in a runtime error.
*   **Integration**: This hook is utilized by `use-websocket.ts` to push incoming system events directly to the UI layer.
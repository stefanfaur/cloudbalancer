# Symbol: web-dashboard.src.api.types.WorkerHealthState

# WorkerHealthState

## Purpose
`WorkerHealthState` is a union type alias that defines the operational status of a worker node within the system. It serves as the single source of truth for representing the health lifecycle of workers, ensuring type safety across the dashboard when displaying status indicators, filtering worker lists, or managing cluster scaling logic.

## Signature
```typescript
type WorkerHealthState = "HEALTHY" | "SUSPECT" | "DEAD" | "RECOVERING" | "DRAINING" | "STOPPING"
```

## Parameters
As a type alias, `WorkerHealthState` does not accept parameters. It is a string literal union type consisting of the following values:

*   **`"HEALTHY"`**: The worker is fully operational and performing tasks as expected.
*   **`"SUSPECT"`**: The worker is exhibiting signs of instability or latency, but has not yet been marked as dead.
*   **`"DEAD"`**: The worker is unresponsive and is no longer processing tasks.
*   **`"RECOVERING"`**: The worker is in the process of restarting or re-joining the cluster after a failure.
*   **`"DRAINING"`**: The worker is finishing existing tasks and will not accept new ones, typically in preparation for maintenance or shutdown.
*   **`"STOPPING"`**: The worker is in the process of shutting down gracefully.

## Returns
Not applicable. This symbol is a type definition and does not return values.

## Example Usage

### Using in a Component
You can use `WorkerHealthState` to ensure that UI components only accept valid status strings:

```tsx
import { WorkerHealthState } from '../api/types';

interface StatusBadgeProps {
  state: WorkerHealthState;
}

export const StatusBadge = ({ state }: StatusBadgeProps) => {
  const colorMap: Record<WorkerHealthState, string> = {
    HEALTHY: 'green',
    SUSPECT: 'yellow',
    DEAD: 'red',
    RECOVERING: 'blue',
    DRAINING: 'orange',
    STOPPING: 'gray'
  };

  return <div style={{ backgroundColor: colorMap[state] }}>{state}</div>;
};
```

### Using in API Logic
The type is widely used across the `web-dashboard` codebase to handle worker data retrieved from the backend:

```typescript
import { WorkerHealthState } from './api/types';

function handleWorkerUpdate(id: string, newState: WorkerHealthState) {
  if (newState === 'DEAD') {
    console.error(`Worker ${id} has failed!`);
    // Trigger alert or cleanup logic
  }
}
```
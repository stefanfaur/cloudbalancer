# Symbol: web-dashboard.src.api.types.TaskState

## Purpose

`TaskState` is a union type alias that defines the exhaustive set of lifecycle statuses for a task within the system. It serves as the single source of truth for task status representation across the `web-dashboard` application, ensuring type safety when handling task transitions, filtering task lists, and rendering status-based UI components.

## Signature

```typescript
type TaskState = "SUBMITTED" | "VALIDATED" | "QUEUED" | "ASSIGNED" | "PROVISIONING" | "RUNNING" | "POST_PROCESSING" | "COMPLETED" | "FAILED" | "TIMED_OUT" | "CANCELLED" | "DEAD_LETTERED"
```

## Parameters

As a type alias, `TaskState` does not accept parameters. It is a literal union type representing the valid string values that a task status can hold.

## Returns

N/A (This is a type definition, not a function).

## Example Usage

### Type Guarding
Use `TaskState` to enforce strict typing in functions that process task statuses:

```typescript
import { TaskState } from 'web-dashboard/src/api/types';

function getStatusColor(state: TaskState): string {
  switch (state) {
    case 'RUNNING':
      return 'blue';
    case 'COMPLETED':
      return 'green';
    case 'FAILED':
    case 'DEAD_LETTERED':
      return 'red';
    default:
      return 'gray';
  }
}
```

### API Integration
Use the type when defining task interfaces to ensure consistency with the backend API:

```typescript
interface Task {
  id: string;
  name: string;
  state: TaskState;
}

const currentTask: Task = {
  id: 'task-123',
  name: 'Data Processing',
  state: 'QUEUED' // Valid
};
```

### Component Props
Use `TaskState` to define props for UI components like status badges:

```tsx
interface StatusBadgeProps {
  state: TaskState;
}

export const StatusBadge = ({ state }: StatusBadgeProps) => {
  return <span className={`badge-${state.toLowerCase()}`}>{state}</span>;
};
```
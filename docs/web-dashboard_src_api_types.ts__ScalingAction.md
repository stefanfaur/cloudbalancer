# Symbol: web-dashboard.src.api.types.ScalingAction

## Purpose

`ScalingAction` is a union type alias used to define the set of permitted operations for the cluster auto-scaling engine. It acts as a strictly typed constraint for state management and API communication, ensuring that only valid scaling commands are processed by the system.

By restricting values to `"SCALE_UP"`, `"SCALE_DOWN"`, or `"NONE"`, this type prevents invalid state transitions and provides better IDE autocompletion and type safety across the dashboard's API and UI components.

## Signature

```typescript
type ScalingAction = "SCALE_UP" | "SCALE_DOWN" | "NONE"
```

## Parameters

*This symbol is a type alias and does not accept parameters.*

## Returns

*This symbol is a type alias and does not return a value.*

## Example Usage

The `ScalingAction` type is widely used across the application to handle API responses and drive UI logic, such as rendering status badges or triggering scaling requests.

### API Integration
```typescript
import { ScalingAction } from '../api/types';

async function updateClusterScale(action: ScalingAction) {
  const response = await fetch('/api/scaling', {
    method: 'POST',
    body: JSON.stringify({ action })
  });
  return response.json();
}

// Valid usage
updateClusterScale("SCALE_UP");
```

### UI Component Logic
```typescript
import { ScalingAction } from '../api/types';

interface BadgeProps {
  action: ScalingAction;
}

export const StatusBadge = ({ action }: BadgeProps) => {
  const color = action === "SCALE_UP" ? "green" : action === "SCALE_DOWN" ? "red" : "gray";
  
  return <div className={`badge-${color}`}>{action}</div>;
};
```
# Symbol: web-dashboard.src.api.types.Priority

## Purpose

The `Priority` type alias defines a strictly constrained set of string literals used to categorize the urgency or importance level of system entities, such as tasks, alerts, or worker processes. By centralizing these values, the application ensures type safety and consistency across the dashboard, preventing invalid priority assignments and enabling predictable UI rendering (e.g., color-coding badges based on the priority level).

## Signature

```typescript
type Priority = "CRITICAL" | "HIGH" | "NORMAL" | "LOW"
```

## Parameters

This symbol is a **type alias** and does not accept parameters. It represents a union of four specific string literals:

*   **`"CRITICAL"`**: Reserved for system-breaking issues or high-urgency tasks requiring immediate attention.
*   **`"HIGH"`**: Indicates tasks or statuses that should be prioritized above standard operations.
*   **`"NORMAL"`**: The default priority level for standard operations.
*   **`"LOW"`**: Used for background tasks or non-urgent informational updates.

## Returns

As a type alias, `Priority` does not return a value. It is used to annotate variables, function arguments, or object properties to enforce that only the defined string literals are assigned.

## Example Usage

### Type Annotation
Use `Priority` to ensure that functions handling task data only accept valid priority levels.

```typescript
import { Priority } from "../api/types";

interface Task {
  id: string;
  name: string;
  priority: Priority;
}

function updateTaskPriority(task: Task, newPriority: Priority) {
  task.priority = newPriority;
}

// Valid usage
updateTaskPriority(myTask, "CRITICAL");

// TypeScript error: Argument of type '"URGENT"' is not assignable to parameter of type 'Priority'
// updateTaskPriority(myTask, "URGENT");
```

### UI Component Integration
Commonly used in components to map priority levels to visual styles:

```tsx
import { Priority } from "../api/types";

interface HealthBadgeProps {
  level: Priority;
}

export const HealthBadge = ({ level }: HealthBadgeProps) => {
  const colorMap = {
    CRITICAL: "bg-red-500",
    HIGH: "bg-orange-500",
    NORMAL: "bg-blue-500",
    LOW: "bg-gray-500",
  };

  return <div className={colorMap[level]}>{level}</div>;
};
```
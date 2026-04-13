# Symbol: web-dashboard.src.api.types.ExecutorType

## Purpose

`ExecutorType` is a union type alias that defines the set of supported execution environments within the system. It acts as a strict contract for components and services that need to identify, filter, or route tasks based on the underlying infrastructure required to run them. By centralizing these values, the application ensures type safety across the dashboard when handling task configurations, worker capabilities, and status reporting.

## Signature

```typescript
type ExecutorType = "SHELL" | "DOCKER" | "PYTHON" | "SIMULATED"
```

## Parameters

This symbol is a type alias and does not accept parameters. It represents one of the following string literals:

*   **`"SHELL"`**: Indicates the task runs directly within the host operating system's shell environment.
*   **`"DOCKER"`**: Indicates the task is containerized and executed within a Docker environment.
*   **`"PYTHON"`**: Indicates the task is executed via a managed Python runtime environment.
*   **`"SIMULATED"`**: Indicates a mock or dry-run execution mode, typically used for testing or development purposes without side effects.

## Returns

As a type alias, `ExecutorType` does not return a value. It is used to annotate variables, function parameters, or object properties to enforce that only valid executor strings are assigned.

## Example Usage

### Type Annotation in Components
```typescript
import { ExecutorType } from "@/api/types";

interface TaskProps {
  id: string;
  executor: ExecutorType;
}

const TaskBadge = ({ executor }: TaskProps) => {
  return <span className={`badge-${executor.toLowerCase()}`}>{executor}</span>;
};
```

### Conditional Logic
```typescript
import { ExecutorType } from "@/api/types";

function getExecutorIcon(type: ExecutorType) {
  switch (type) {
    case "DOCKER":
      return <DockerIcon />;
    case "PYTHON":
      return <PythonIcon />;
    case "SHELL":
      return <TerminalIcon />;
    case "SIMULATED":
      return <FlaskIcon />;
  }
}
```
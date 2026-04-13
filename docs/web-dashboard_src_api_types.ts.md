# File: web-dashboard/src/api/types.ts

## Overview

`web-dashboard/src/api/types.ts` is the central schema definition file for the entire `web-dashboard` application. It acts as the single source of truth for all data structures, API contracts, and domain-specific enumerations used across the frontend.

**⚠️ HOTSPOT WARNING:** This file is a critical dependency for almost every module in the application. It ranks in the top 25% for both change frequency and complexity. Modifications to these types can trigger cascading type errors across the codebase. Exercise extreme caution when refactoring or extending existing interfaces.

## Public API

### Enums & Union Types
*   **`TaskState`**: Represents the lifecycle of a task (e.g., `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`).
*   **`Priority`**: Defines task urgency (`CRITICAL`, `HIGH`, `NORMAL`, `LOW`).
*   **`ExecutorType`**: Specifies the runtime environment (`SHELL`, `DOCKER`, `PYTHON`, `SIMULATED`).
*   **`WorkerHealthState`**: Defines node operational status (`HEALTHY`, `SUSPECT`, `DRAINING`, etc.).
*   **`Role`**: User authorization levels (`ADMIN`, `OPERATOR`, `VIEWER`).
*   **`ScalingAction`**: Cluster auto-scaling commands (`SCALE_UP`, `SCALE_DOWN`, `NONE`).

### Core Interfaces
*   **`TaskDescriptor`**: The primary contract for task submission, containing execution specs, resource requirements, and policies.
*   **`TaskEnvelope`**: The wrapper object for a task, including its current state and full execution history.
*   **`WorkerMetricsSnapshot` / `WorkerMetricsBucket`**: Data structures for real-time and aggregated worker performance monitoring.
*   **`ScalingPolicy` / `ScalingStatusResponse`**: Configuration and status objects for the cluster auto-scaling engine.
*   **`WsMessage`**: A discriminated union type defining all possible messages transmitted over the WebSocket connection.

## Dependencies

This file is a foundational module and does not import from other project files. It relies solely on standard TypeScript primitives and built-in types.

## Usage Notes

### Type Safety and Discriminated Unions
When handling `WsMessage`, always use a `switch` statement or `if` checks on the `type` property to leverage TypeScript's type narrowing. This ensures that the `payload` is correctly typed for each specific message variant.

### Example: Handling Task Updates
```typescript
import { TaskEnvelope, WsMessage } from './api/types';

function handleMessage(msg: WsMessage) {
  switch (msg.type) {
    case "TASK_UPDATE":
      // msg.payload is automatically narrowed to TaskEnvelope
      updateTaskUI(msg.payload);
      break;
    case "ALERT":
      // msg.payload is narrowed to { severity, message, timestamp }
      showNotification(msg.payload.message);
      break;
  }
}
```

### Best Practices for Hotspot Maintenance
1.  **Backward Compatibility**: When adding new fields to interfaces like `TaskDescriptor` or `WorkerMetricsSnapshot`, mark them as optional (`?`) to prevent breaking existing API consumers.
2.  **Validation**: Since these types represent API contracts, ensure that the backend responses strictly adhere to these schemas. Use runtime validation libraries (like Zod) in conjunction with these types if you need to verify API responses at runtime.
3.  **Refactoring**: If you need to change a union type (e.g., adding a new `TaskState`), verify the impact on `status-badge.tsx` and `lifecycle-timeline.tsx`, as these components likely contain logic that depends on exhaustive union matching.
4.  **Documentation**: Always update the docstrings in this file when adding new fields to ensure the team understands the intent behind new metrics or configuration options.
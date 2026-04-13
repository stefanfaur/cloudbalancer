# File: web-dashboard/src/components/lifecycle-timeline.tsx

## Overview

The `LifecycleTimeline` component provides a visual representation of a task's progression through its defined lifecycle stages. It renders a horizontal timeline of states, highlighting the current progress, completed stages, and pending steps. It is designed to provide immediate visual feedback on where a task sits within the system's execution flow.

## Public API

### `LifecycleTimelineProps`
The component accepts the following properties:

| Property | Type | Description |
| :--- | :--- | :--- |
| `executionHistory` | `ExecutionAttempt[]` | An array of past execution attempts used to derive timing information. |
| `currentState` | `TaskState` | The current status of the task, determining which step is highlighted as active. |

### `stateColor`
A utility function used to determine the CSS class for the timeline nodes.

*   **Signature**: `function stateColor(state: TaskState, isCurrent: boolean, isPast: boolean): string`
*   **Logic**:
    *   If `isCurrent`: Returns `bg-emerald-500` for completed tasks, `bg-red-500` for terminal error states, or `bg-blue-500` (with a pulse animation) for active states.
    *   If `isPast`: Returns `bg-slate-500`.
    *   Default: Returns `bg-slate-700`.

## Dependencies

*   **`date-fns`**: Used for formatting the `startedAt` timestamp of the last execution attempt.
*   **`@/lib/utils`**: Provides the `cn` utility for conditional tailwind class merging.
*   **`@/api/types`**: Provides the `ExecutionAttempt` and `TaskState` type definitions.

## Usage Notes

*   **Lifecycle Order**: The component uses a hardcoded `LIFECYCLE_ORDER` constant to define the sequence of states: `SUBMITTED` -> `VALIDATED` -> `QUEUED` -> `ASSIGNED` -> `PROVISIONING` -> `RUNNING` -> `POST_PROCESSING` -> `COMPLETED`.
*   **Terminal States**: The component recognizes specific terminal states (`COMPLETED`, `FAILED`, `TIMED_OUT`, `CANCELLED`, `DEAD_LETTERED`). If a task reaches a terminal state, the timeline adjusts to reflect the finality of the process.
*   **Visual Feedback**: The component displays the time elapsed since the last attempt started when the task is in an active state, providing context on how long the current step has been running.
*   **Integration**: This component is primarily utilized in `web-dashboard/src/pages/tasks/task-detail.tsx` to visualize individual task progress.
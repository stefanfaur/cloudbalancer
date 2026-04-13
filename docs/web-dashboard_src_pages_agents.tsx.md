# File: web-dashboard/src/pages/agents.tsx

## Overview

The `web-dashboard/src/pages/agents.tsx` file defines the UI for the Agent Management dashboard. It provides a tabular view of all connected agents, displaying real-time resource utilization (CPU and Memory), worker counts, supported executors, and heartbeat status. The page also includes administrative controls to trigger scaling actions (Scale Up/Down) for individual agents.

## Public API

### `timeAgo`
```typescript
function timeAgo(iso: string | null): string
```
A utility function that converts an ISO timestamp string into a human-readable relative time string (e.g., "45s ago", "2m ago"). Returns "—" if the input is null.

### `CapacityBar`
```typescript
function CapacityBar({ used, total, label }: { used: number; total: number; label: string }): JSX.Element
```
A visual progress component that renders a resource usage bar. 
- **Color Logic**: 
  - `emerald-500` (<= 60%)
  - `amber-500` (61% - 85%)
  - `red-500` (> 85%)

### `AgentRow`
```typescript
function AgentRow({ agent }: { agent: AgentInfoResponse }): JSX.Element
```
A table row component representing a single agent. It calculates resource consumption from the provided `AgentInfoResponse` and provides buttons to trigger scaling mutations via the `useTriggerScaling` hook.

## Dependencies

- **API Hooks**:
  - `useAgents`: Fetches the list of connected agents.
  - `useTriggerScaling`: Executes scaling operations (Scale Up/Down).
- **UI Components**:
  - `@/components/ui/table`: Used for the main data grid.
  - `@/components/ui/button`: Used for action triggers.
  - `@/components/ui/skeleton`: Used for loading states.
  - `@/components/error-card`: Displays error states if data fetching fails.
- **Icons**: `lucide-react` (Cpu, ChevronUp, ChevronDown).

## Usage Notes

- **Data Fetching**: The page uses the `useAgents` hook to manage data state. It automatically handles `isLoading`, `isError`, and empty states.
- **Scaling Actions**: The "Scale Up" and "Scale Down" buttons are disabled while a mutation is pending (`trigger.isPending`) to prevent concurrent request conflicts.
- **Responsive Design**: The `CapacityBar` is fixed-width (`w-24`) to ensure consistent alignment within the table cells.
- **Error Handling**: If the agent list fails to load, the `ErrorCard` component is rendered, providing a retry mechanism via the `refetch` function.
# File: web-dashboard/src/api/agents.ts

## Overview

`web-dashboard/src/api/agents.ts` provides the data fetching layer for agent-related information within the web dashboard. It leverages `react-query` to manage server state, caching, and synchronization for the cluster's agent nodes.

## Public API

### `AgentInfoResponse` (Interface)
Defines the structure of the data returned by the agent API endpoint.

| Property | Type | Description |
| :--- | :--- | :--- |
| `agentId` | `string` | Unique identifier for the agent. |
| `hostname` | `string` | The network hostname of the agent node. |
| `totalCpuCores` | `number` | Total CPU capacity. |
| `availableCpuCores` | `number` | Currently available CPU capacity. |
| `totalMemoryMB` | `number` | Total system memory in Megabytes. |
| `availableMemoryMB` | `number` | Currently available memory in Megabytes. |
| `activeWorkerIds` | `string[]` | List of worker IDs currently running on this agent. |
| `supportedExecutors` | `string[]` | List of execution environments supported by the agent. |
| `lastHeartbeat` | `string \| null` | ISO timestamp of the last successful heartbeat. |

### `useAgents()` (Function)
A React hook that fetches the list of all cluster agents.

- **Returns**: A `UseQueryResult` containing an array of `AgentInfoResponse` objects.
- **Query Key**: `["agents"]`
- **Behavior**: Automatically fetches data from `/api/admin/agents` using the internal `apiFetch` client.

## Dependencies

- `@tanstack/react-query`: Used for managing asynchronous server state and caching.
- `web-dashboard/src/api/client.ts`: Provides the `apiFetch` utility for authenticated HTTP requests.

## Usage Notes

- **Caching**: The `useAgents` hook is configured with a `staleTime` of 15,000ms (15 seconds). Data will be considered fresh for this duration before a background refetch is triggered upon component re-mount or window focus.
- **Integration**: This hook is primarily consumed by `web-dashboard/src/pages/agents.tsx` to populate the agent monitoring dashboard.
- **Error Handling**: As this hook returns a standard `react-query` object, consumers should handle the `isLoading`, `isError`, and `error` states to provide appropriate UI feedback.
# File: web-dashboard/src/api/admin.ts

## Overview

`web-dashboard/src/api/admin.ts` serves as the centralized administrative API layer for the web dashboard. It provides a suite of React Query hooks to manage system-wide configurations, agent authentication tokens, worker registry state, and chaos engineering utilities. This module facilitates administrative control over the infrastructure, allowing for real-time strategy updates, worker management, and fault injection testing.

## Public API

### Strategy Management
*   `useStrategy()`: Fetches the current system strategy configuration.
*   `useUpdateStrategy()`: Updates the system strategy and associated weights.

### Worker Management
*   `useUpdateWorkerTags()`: Updates tags for a specific worker.
*   `useWorkerRegistry()`: Retrieves the current database state of all registered workers.
*   `WorkerRegistryEntry` (Interface): Defines the structure of a worker registry record, including health status and active task counts.

### Agent Token Management
*   `useAgentTokens()`: Fetches a list of all agent tokens.
*   `useCreateAgentToken()`: Generates a new agent token with a specified label.
*   `useRevokeAgentToken()`: Revokes an existing agent token by ID.
*   `AgentTokenSummary` (Interface): Defines the metadata for an agent token, including creation and usage timestamps.

### Chaos Engineering
*   `useKillWorker()`: Triggers a worker termination event.
*   `useFailTask()`: Forces a specific task to fail.
*   `useInjectLatency()`: Injects artificial network latency into the system for testing purposes.

## Dependencies

*   **@tanstack/react-query**: Used for server state management, caching, and mutation handling.
*   **./client**: Imports `apiFetch` for standardized HTTP communication.
*   **./types**: Imports shared type definitions, specifically `StrategyResponse`.

## Usage Notes

*   **Caching**: Queries like `useStrategy`, `useAgentTokens`, and `useWorkerRegistry` utilize `staleTime` to balance data freshness with performance. Ensure these values align with the expected volatility of the underlying data.
*   **Invalidation**: Mutation hooks (e.g., `useUpdateStrategy`, `useCreateAgentToken`) automatically invalidate relevant query keys upon success to ensure the UI remains synchronized with the server state.
*   **Chaos Utilities**: The `useKillWorker`, `useFailTask`, and `useInjectLatency` hooks are intended for administrative testing. Use these with caution in production environments as they directly impact system stability and task execution.
*   **Error Handling**: All hooks rely on the underlying `apiFetch` implementation; ensure that global error handling or specific component-level error boundaries are configured to manage potential API failures.
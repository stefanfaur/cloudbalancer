# File: web-dashboard/src/api/scaling.ts

## Overview

`web-dashboard/src/api/scaling.ts` provides a set of React Query hooks designed to interface with the system's auto-scaling engine. It acts as the primary data-fetching and mutation layer for managing cluster scaling policies, monitoring status, and triggering manual scaling actions.

## Public API

### `useScalingStatus()`
Retrieves the current status of the cluster scaling engine.
- **Returns**: A `UseQueryResult` containing `ScalingStatusResponse`.
- **Behavior**: Caches the result for 30 seconds (`staleTime: 30_000`) to minimize redundant network requests.

### `useUpdateScalingPolicy()`
Updates the configuration for the cluster scaling policy.
- **Parameters**: `policy` (a `Partial<ScalingPolicy>` object).
- **Behavior**: Performs a `PUT` request to `/api/scaling/policy`. Upon success, it automatically invalidates the `["scaling-status"]` query cache to ensure the UI reflects the updated policy.

### `useTriggerScaling()`
Manually triggers a scaling action on the cluster.
- **Parameters**: An object containing `action` (string), and optional `count` (number) or `agentId` (string).
- **Behavior**: Performs a `POST` request to `/api/scaling/trigger`. Upon success, it invalidates the `["scaling-status"]` query cache to refresh the current status.

## Dependencies

- **@tanstack/react-query**: Used for managing server state, caching, and mutation lifecycle.
- **./client**: Imports `apiFetch` for standardized HTTP communication.
- **./types**: Imports `ScalingStatusResponse` and `ScalingPolicy` for type safety.

## Usage Notes

- **Cache Invalidation**: Both `useUpdateScalingPolicy` and `useTriggerScaling` are configured to automatically invalidate the `scaling-status` query upon success. This ensures that any component observing the scaling status will receive fresh data immediately after a state change.
- **Integration**: This module is primarily consumed by administrative pages such as `settings.tsx` (for policy management) and `agents.tsx` (for manual scaling triggers).
- **Error Handling**: As these hooks utilize `apiFetch`, ensure that the global API client is configured to handle non-2xx HTTP responses appropriately to allow React Query to manage error states.
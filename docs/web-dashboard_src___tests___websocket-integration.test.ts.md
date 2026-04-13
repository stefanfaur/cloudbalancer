# File: web-dashboard/src/__tests__/websocket-integration.test.ts

## Overview

`web-dashboard/src/__tests__/websocket-integration.test.ts` is a test suite designed to validate the mapping logic between incoming WebSocket message types and their corresponding React Query cache keys. 

This file ensures that when specific events occur on the backend (e.g., a task update or worker state change), the frontend correctly identifies which cached data needs to be invalidated or refetched. It acts as a bridge between the raw WebSocket event stream and the application's data synchronization layer.

## Public API

This file does not export any functions or classes. It contains a private constant `MESSAGE_TYPE_TO_QUERY_KEYS` which defines the mapping schema:

- **`MESSAGE_TYPE_TO_QUERY_KEYS`**: A `Record<string, string[]>` where keys represent WebSocket message types and values represent arrays of React Query cache keys to be invalidated upon receipt of that message.

## Dependencies

- **`vitest`**: Used as the testing framework for executing the suite and performing assertions.

## Usage Notes

- **Purpose**: This test file is intended to prevent regressions in cache invalidation logic. If a new WebSocket message type is added to the system, it must be evaluated to determine if it requires cache invalidation.
- **Adding New Messages**: 
    - If a new message type requires cache invalidation, update the `MESSAGE_TYPE_TO_QUERY_KEYS` object in the source code and add a corresponding test case in this file.
    - If a new message type is added that does *not* require cache invalidation (similar to the `ALERT` type), ensure the `all known message types have cache mappings` test is updated to reflect the new type in the `knownTypes` array.
- **Scope**: This is a unit-level integration test. It does not test the actual WebSocket connection or network transport; those are covered by `use-websocket` unit tests and E2E test suites.
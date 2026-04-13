# File: web-dashboard/src/hooks/__tests__/use-websocket.test.ts

## Overview

`web-dashboard/src/hooks/__tests__/use-websocket.test.ts` is a test suite designed to validate the logic and integration of WebSocket communication within the `web-dashboard` application. It includes a custom `MockWebSocket` implementation to simulate browser WebSocket behavior in a Node.js/Vitest environment, ensuring that connection handling, message parsing, and reconnection strategies function as expected without requiring a live server.

## Public API

### `MockWebSocket` (Class)
A mock implementation of the standard browser `WebSocket` interface used for testing hook logic.

*   **`constructor(url: string)`**: Initializes a new mock instance, records the URL, and pushes the instance to `MockWebSocket.instances`. It simulates an asynchronous connection by updating `readyState` to `1` (OPEN) after a 10ms delay.
*   **`close()`**: Synchronously updates the `readyState` to `3` (CLOSED).
*   **`send(_data: string)`**: A no-op method provided to satisfy the `WebSocket` interface requirements.

**Static Properties:**
*   **`instances: MockWebSocket[]`**: A registry of all `MockWebSocket` instances created during a test run, allowing for assertions on connection state and URL parameters.

## Dependencies

*   **`vitest`**: Used for the test runner, assertion library, and mocking utilities (`vi`).

## Usage Notes

*   **Global Mocking**: The test suite uses `vi.stubGlobal("WebSocket", MockWebSocket)` in the `beforeEach` block to intercept standard WebSocket instantiations within the application code.
*   **Message Validation**: The suite contains specific test cases to verify that the application correctly parses incoming JSON payloads for the following event types:
    *   `TASK_UPDATE`
    *   `WORKER_STATE`
    *   `SCALING_EVENT`
*   **Reconnection Logic**: The suite includes a unit test verifying the exponential backoff algorithm, ensuring that reconnection delays follow the expected sequence: `1000ms`, `2000ms`, `4000ms`, `8000ms`, and `16000ms`.
*   **Cleanup**: `MockWebSocket.instances` is cleared before each test to prevent state leakage between test cases. `vi.restoreAllMocks()` is called after each test to ensure a clean global environment.
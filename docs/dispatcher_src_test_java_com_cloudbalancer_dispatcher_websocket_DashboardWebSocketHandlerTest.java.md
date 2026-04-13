# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/websocket/DashboardWebSocketHandlerTest.java

## Overview

`DashboardWebSocketHandlerTest` is a comprehensive unit test suite for the `DashboardWebSocketHandler` class. It validates the server-side WebSocket lifecycle, message broadcasting, and state synchronization logic.

**Note:** This file is a **HOTSPOT** within the `dispatcher` module. It exhibits high change frequency and complexity, making it a high-risk area for regressions. Developers should exercise extreme caution when modifying the underlying `DashboardWebSocketHandler` logic, as WebSocket state management is sensitive to concurrency and connection lifecycle events.

## Public API

The test class does not expose a public API but validates the following methods of `DashboardWebSocketHandler`:

*   `afterConnectionEstablished(WebSocketSession session)`: Ensures that a new connection triggers an `INITIAL_SNAPSHOT` message containing current task counts.
*   `broadcast(String type, Object payload)`: Verifies that messages are correctly serialized and dispatched to all active sessions.
*   `afterConnectionClosed(WebSocketSession session, CloseStatus status)`: Confirms that sessions are correctly removed from the internal registry to prevent memory leaks or attempts to send messages to closed sockets.

## Dependencies

*   **JUnit 5**: Used as the primary testing framework.
*   **Mockito**: Used for mocking `WebSocketSession`, `WorkerRegistryService`, and `TaskRepository`.
*   **AssertJ**: Used for fluent assertion syntax.
*   **Jackson**: Used for parsing and verifying the JSON structure of outgoing WebSocket messages.
*   **Internal Components**:
    *   `TaskRepository`: Mocked to simulate database state (task counts).
    *   `WorkerRegistryService`: Mocked to simulate worker state.

## Usage Notes

### Testing Lifecycle and Edge Cases
The test suite covers several critical edge cases that are common sources of bugs in WebSocket implementations:

1.  **Resilience to IOExceptions**: The `ioExceptionOnOneSessionDoesNotPreventDeliveryToOthers` test ensures that if a single client connection fails during a broadcast (e.g., due to a network drop), the handler continues to process and deliver messages to remaining healthy sessions.
2.  **State Synchronization**: The `initialSnapshotSentOnConnection` test verifies that the server pushes the current system state (queued/running task counts) immediately upon handshake, ensuring the dashboard UI is populated without waiting for the next broadcast event.
3.  **Session Cleanup**: The `sessionRemovedAfterClose` test validates that the handler correctly manages its internal session registry, preventing "ghost" sessions that could lead to `IOException` spam or memory leaks.

### How to Run
To execute these tests, use the standard Maven lifecycle command from the project root:
```bash
mvn test -Dtest=DashboardWebSocketHandlerTest
```

### Common Pitfalls
*   **Mocking `WebSocketSession`**: When writing new tests for this handler, ensure that `session.isOpen()` is explicitly mocked. The handler relies on this check before attempting to send data; failing to mock it will result in tests that do not accurately reflect production behavior.
*   **Message Ordering**: Because `afterConnectionEstablished` sends an initial snapshot, any subsequent `broadcast` call will result in multiple messages being sent to the session. Always use `ArgumentCaptor` or `verify(..., times(n))` to account for the initial snapshot message.
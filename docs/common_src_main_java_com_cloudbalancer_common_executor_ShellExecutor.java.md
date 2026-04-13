# File: common/src/main/java/com/cloudbalancer/common/executor/ShellExecutor.java

## Overview

The `ShellExecutor` is a core component of the CloudBalancer infrastructure, responsible for executing arbitrary shell commands in a controlled environment. It implements the `TaskExecutor` interface to provide a standardized way to run system-level tasks, manage their lifecycles, and capture output streams.

**Note:** This file is a **HOTSPOT** within the repository. It exhibits high change frequency and complexity. As it handles system-level process execution, it is a high-risk area for security vulnerabilities and resource management bugs.

## Public API

### Constructors
*   **`ShellExecutor()`**: Initializes the executor with default security settings (a predefined list of blocked commands) and a 1MB output buffer limit.
*   **`ShellExecutor(Set<String> blockedCommands, int maxOutputBytes)`**: Allows custom configuration of security constraints and output stream size limits.

### Methods
*   **`execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`**: Executes the command defined in the `spec`. It manages process creation, environment variable injection, and asynchronous stream reading.
*   **`validate(Map<String, Object> spec)`**: Performs security checks against the command string to prevent execution of dangerous or unauthorized system commands.
*   **`cancel(ExecutionHandle handle)`**: Terminates a running process associated with the provided task ID using `destroyForcibly()`.
*   **`getCapabilities()`**: Returns the `ExecutorCapabilities`, defining the executor's resource profile and `SANDBOXED` security level.
*   **`getExecutorType()`**: Returns `ExecutorType.SHELL`.
*   **`estimateResources(Map<String, Object> spec)`**: Provides a static resource estimation for scheduling purposes.

## Dependencies

The `ShellExecutor` relies on the following internal and external components:
*   **`com.cloudbalancer.common.model`**: Provides data structures for `ExecutorCapabilities`, `ExecutorType`, `ResourceProfile`, and `SecurityLevel`.
*   **`java.util.concurrent`**: Utilized for managing process tracking (`ConcurrentHashMap`) and asynchronous stream processing (`CompletableFuture`).
*   **`java.io`**: Used for `ProcessBuilder` and stream handling (`BufferedReader`).

## Usage Notes

### Security and Validation
The `ShellExecutor` includes a built-in blacklist to prevent common destructive commands (e.g., `rm -rf /`, `shutdown`). 
*   **Warning**: The `validate` method performs a simple string containment check. Ensure that the `blockedCommands` set is sufficiently robust for your deployment environment.
*   **Sandboxing**: The executor reports a `SecurityLevel.SANDBOXED` capability, but actual isolation depends on the underlying host OS configuration.

### Resource Management
*   **Output Buffering**: To prevent memory exhaustion, the executor enforces a `maxOutputBytes` limit. If a process exceeds this limit, the remaining output is discarded.
*   **Process Lifecycle**: Processes are tracked in a `ConcurrentHashMap` keyed by `UUID`. Always ensure that `cancel` is called if a task is aborted to prevent orphaned processes.

### Example Usage
```java
// Initialize with default settings
ShellExecutor executor = new ShellExecutor();

// Define task specification
Map<String, Object> spec = Map.of("command", "ls -la /tmp");

// Execute task
ExecutionResult result = executor.execute(spec, allocation, context);

if (result.exitCode() == 0) {
    System.out.println("Output: " + result.stdout());
} else {
    System.err.println("Error: " + result.stderr());
}
```

### Potential Pitfalls
*   **Blocking Calls**: The `execute` method calls `process.waitFor()`, which blocks the calling thread. Ensure this is invoked within an appropriate thread pool to avoid starving the dispatcher.
*   **Interruption**: If the thread executing `execute` is interrupted, the process is destroyed forcibly. Always handle `InterruptedException` gracefully in the calling context.
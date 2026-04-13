# File: common/src/test/java/com/cloudbalancer/common/executor/PythonExecutorTest.java

## Overview

`PythonExecutorTest` is a comprehensive JUnit 5 test suite designed to validate the `PythonExecutor` implementation. This class is a **critical hotspot** within the `common` module, exhibiting high change frequency and complexity. It serves as the primary verification layer for the Python runtime environment integration, ensuring that scripts are validated, executed, and managed within the `cloudbalancer` infrastructure.

The test suite covers the full lifecycle of a Python task, including input validation, resource estimation, standard stream capture, logging callbacks, virtual environment handling, output truncation, and process termination.

## Public API

The `PythonExecutorTest` class does not expose a public API for production use; it is strictly a test suite. However, it exercises the following key methods of the `PythonExecutor` class:

*   **`validate(Map<String, Object> spec)`**: Ensures the executor rejects malformed or empty scripts and accepts valid ones.
*   **`getExecutorType()`**: Verifies the executor correctly identifies as `ExecutorType.PYTHON`.
*   **`getCapabilities()`**: Confirms the executor operates in a `SANDBOXED` security level without requiring Docker.
*   **`estimateResources(Map<String, Object> spec)`**: Validates that the executor provides non-zero resource defaults for CPU and memory.
*   **`execute(Map<String, Object> spec, ResourceAllocation alloc, TaskContext ctx)`**: The primary execution method. Tests verify stdout/stderr capture, syntax error handling, log callback invocation, and virtual environment creation.
*   **`cancel(ExecutionHandle handle)`**: Verifies that the executor can successfully terminate long-running Python processes.

## Dependencies

The test suite relies on the following components:

*   **JUnit 5 (`org.junit.jupiter.api`)**: Framework for test execution and lifecycle management.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent, readable assertions.
*   **`TaskContext` / `ExecutionResult`**: Domain models representing the execution environment and outcome.
*   **`java.nio.file.Path`**: Used with `@TempDir` to provide isolated, temporary workspaces for script execution.
*   **`java.util.concurrent`**: Utilized to test asynchronous execution and process cancellation.

## Usage Notes

### Hotspot Warning
As a high-activity file, changes to `PythonExecutor` or its underlying process management logic often require updates to this test suite. Failure to maintain these tests can lead to silent regressions in task execution or resource leaks.

### Testing Best Practices
*   **Isolated Workspaces**: Every test method that performs execution uses the `@TempDir` annotation. This ensures that virtual environments created during tests do not pollute the host system or interfere with parallel test execution.
*   **Asynchronous Handling**: The `cancelKillsPythonProcess` test demonstrates the use of `CompletableFuture` to simulate real-world task cancellation. When modifying cancellation logic, ensure that the `ExecutionHandle` correctly maps to the underlying OS process ID.
*   **Output Truncation**: The `executeTruncatesLargeOutput` test verifies a safety mechanism. If you modify the buffer size or truncation logic in `PythonExecutor`, ensure this test is updated to reflect the new threshold (currently 1MB).
*   **Log Callbacks**: The `executeInvokesLogCallback` test ensures that real-time logging is functional. If implementing custom log streaming, ensure the `LogCallback` interface remains thread-safe.

### Example: Adding a New Test Case
To test a new feature (e.g., environment variable injection), follow this pattern:

```java
@Test
void executeSetsEnvironmentVariables(@TempDir Path workDir) {
    Map<String, Object> spec = Map.of("script", "import os; print(os.environ.get('TEST_VAR'))");
    // Setup context with env vars
    var ctx = new TaskContext(UUID.randomUUID(), workDir);
    ctx.setEnv("TEST_VAR", "success");
    
    ExecutionResult result = executor.execute(spec, new ResourceAllocation(1, 256, 100), ctx);
    assertThat(result.stdout()).contains("success");
}
```
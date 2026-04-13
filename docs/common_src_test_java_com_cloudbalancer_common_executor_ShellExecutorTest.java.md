# File: common/src/test/java/com/cloudbalancer/common/executor/ShellExecutorTest.java

## Overview

`ShellExecutorTest` is a critical JUnit 5 test suite that validates the `ShellExecutor` component within the CloudBalancer infrastructure. This class is responsible for executing arbitrary shell commands, managing process lifecycles, enforcing security constraints, and handling output streaming.

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Modifications to the `ShellExecutor` logic or this test suite carry a high risk of introducing regressions in task execution, security, or resource cleanup.

## Public API

The test suite validates the following core behaviors of the `ShellExecutor` class:

*   **`validate(Map<String, Object> spec)`**: Ensures that command specifications are checked for blacklisted commands and required parameters.
*   **`execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`**: Validates the primary execution flow, including environment variable injection, working directory isolation, and exit code handling.
*   **`cancel(ExecutionHandle handle)`**: Verifies that long-running processes are correctly terminated to prevent zombie processes.
*   **`estimateResources(Map<String, Object> spec)`**: Confirms that the executor provides sensible default resource requirements for scheduling.
*   **`getCapabilities()`**: Checks that the executor correctly reports its security level (e.g., `SANDBOXED`) and operational requirements (e.g., `requiresDocker`).

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **AssertJ**: Provides fluent assertions for verifying execution results.
*   **`com.cloudbalancer.common.model`**: Relies on domain models including `TaskContext`, `ResourceAllocation`, `ExecutionResult`, and `LogCallback`.
*   **`java.nio.file`**: Utilized for managing temporary working directories during test execution.

## Usage Notes

### Testing Execution Logic
When adding new tests for `ShellExecutor`, always use the `@TempDir` annotation to inject a unique working directory. This prevents cross-test contamination and ensures that file-system-dependent commands (like `cat` or `ls`) operate in a clean environment.

### Handling Asynchronous Processes
Several tests (e.g., `executeCancelledProcessReturnsNonZero`) use `CompletableFuture` to simulate concurrent execution. 
1.  **Start the process**: Wrap the `executor.execute` call in a `CompletableFuture.supplyAsync`.
2.  **Synchronization**: Use `Thread.sleep()` or `Awaitility` to ensure the process has reached a running state before calling `executor.cancel`.
3.  **Cleanup**: Always verify that the `ExecutionResult` returned by the future reflects the cancellation (non-zero exit code).

### Output Truncation
The `ShellExecutor` enforces a maximum byte limit on output to prevent memory exhaustion. When testing this, ensure the command generates enough output (e.g., using `head -c 2000 /dev/zero`) to trigger the truncation logic defined in the executor's constructor.

### LogCallback Integration
The `LogCallback` interface allows real-time streaming of stdout/stderr. Tests must verify:
*   **Line-by-line delivery**: Ensure the callback is invoked for every newline character.
*   **Stream differentiation**: Use the `isStderr` boolean flag in the callback to verify that error logs are correctly identified and captured separately from standard output.

### Potential Pitfalls
*   **Zombie Processes**: If `cancel()` is not implemented correctly in the source, tests will hang or leave orphaned processes on the build agent. Always ensure `cancel` is tested for both responsiveness and effectiveness.
*   **Environment Variables**: When testing environment injection, ensure that the shell environment is correctly sanitized to prevent command injection vulnerabilities.
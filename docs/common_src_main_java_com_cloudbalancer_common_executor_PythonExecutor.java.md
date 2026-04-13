# File: common/src/main/java/com/cloudbalancer/common/executor/PythonExecutor.java

## Overview

The `PythonExecutor` is a core component of the CloudBalancer system, responsible for executing Python scripts in isolated, ephemeral environments. 

**Warning: This file is a system HOTSPOT.** It is in the top 25% for both change frequency and complexity. Modifications to this class carry a high risk of introducing regressions in task isolation, resource cleanup, or process management.

The executor automatically manages the lifecycle of Python scripts by:
1. Creating a temporary directory for each execution.
2. Initializing a local Python virtual environment (`venv`).
3. Installing specified dependencies via `pip`.
4. Executing the script with restricted environment variables.
5. Performing cleanup of all temporary artifacts upon completion.

## Public API

### Constructors
- `PythonExecutor(String pythonBinary)`: Initializes the executor with a specific path to the Python interpreter.
- `PythonExecutor()`: Default constructor, defaults to `python3`.

### Key Methods
- `ValidationResult validate(Map<String, Object> spec)`: Ensures the task specification contains a valid `script` string.
- `ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`: The primary entry point for running a task. It handles the full lifecycle from `venv` creation to script execution and log streaming.
- `void cancel(ExecutionHandle handle)`: Forcibly terminates a running Python process associated with the given handle.
- `ExecutorCapabilities getCapabilities()`: Returns the resource profile and security level (currently `SANDBOXED`).

## Dependencies

- **Internal**: `com.cloudbalancer.common.model.*` (Executor types, resource profiles, and security models).
- **Java Standard Library**: `java.nio.file` (File system operations), `java.util.concurrent` (Process management and async logging), `java.lang.ProcessBuilder` (Subprocess orchestration).

## Usage Notes

### Execution Lifecycle
1. **Validation**: The `spec` map must contain a `script` key.
2. **Environment Setup**: A unique temporary directory is created. A `venv` is initialized inside this directory.
3. **Dependency Management**: If a `requirements` list is provided, `pip install` is executed within the `venv`.
4. **Isolation**: 
   - On Linux, if `networkAccessRequired` is false and the process is not already inside a container, the executor uses `unshare --net` to provide network namespace isolation.
   - Environment variables (`PATH`, `HOME`, `TMPDIR`, `VIRTUAL_ENV`) are strictly controlled to prevent leakage into the host system.
5. **Cleanup**: The `finally` block ensures `deleteDirectoryQuietly` is called, removing the temporary directory and all installed packages, regardless of execution success or failure.

### Implementation Pitfalls
- **Resource Limits**: The `MAX_OUTPUT_BYTES` constant is set to 1MB. If a script produces more than 1MB of combined stdout/stderr, the output will be truncated to prevent memory exhaustion.
- **Process Cleanup**: `cancel` uses `process.destroyForcibly()`. If a script spawns child processes, they may become orphaned unless the script handles signal propagation correctly.
- **Hotspot Risk**: Because this class manages external processes and file system operations, ensure that any changes to `runProcess` or `deleteDirectoryQuietly` are thoroughly tested for edge cases like file locking or zombie processes.

### Example Usage
```java
// Initialize the executor
PythonExecutor executor = new PythonExecutor("/usr/bin/python3");

// Prepare task specification
Map<String, Object> spec = Map.of(
    "script", "print('Hello World')",
    "requirements", List.of("requests==2.28.1"),
    "networkAccessRequired", false
);

// Execute (usually called by the TaskDispatcher)
ExecutionResult result = executor.execute(spec, allocation, context);

if (result.exitCode() == 0) {
    System.out.println("Output: " + result.stdout());
} else {
    System.err.println("Error: " + result.stderr());
}
```
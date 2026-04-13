# File: worker/src/test/java/com/cloudbalancer/worker/service/PythonArtifactIntegrationTest.java

## Overview

`PythonArtifactIntegrationTest` is a critical integration test suite that validates the end-to-end artifact lifecycle within the `worker` module. It ensures the system correctly handles the staging of input artifacts, the execution of Python scripts that consume these artifacts, and the subsequent collection of output artifacts.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. It serves as a primary verification point for the worker's execution pipeline and is a high-risk area for regressions related to file I/O, process execution, and artifact handling.

The suite operates without a Spring context or external services, requiring only `python3` to be present on the system `PATH`.

## Public API

The class provides the following test methods to verify the pipeline:

*   `inlineArtifactStagedAndReadByPythonScript(@TempDir Path workDir)`: Verifies that an inline artifact is correctly staged, read by a Python script, transformed, and collected as an output.
*   `multipleArtifactsStagedAndCollected(@TempDir Path workDir)`: Verifies that multiple input artifacts can be staged, processed together by a script, and that the system handles partial output collection (ignoring missing files).
*   `scriptFailureDoesNotPreventOutputCollection(@TempDir Path workDir)`: Verifies that if a Python script fails (non-zero exit code) after writing output files, the worker still successfully collects the generated artifacts.

## Dependencies

The test relies on the following components:

*   **`PythonExecutor`**: Used to execute the Python scripts within the integration tests.
*   **`ArtifactService`**: Responsible for the staging and collection of artifacts.
*   **`TaskContext` & `ResourceAllocation`**: Used to configure the execution environment for the `PythonExecutor`.
*   **JUnit 5 (`@TempDir`)**: Provides temporary directory management for file I/O operations during tests.
*   **AssertJ**: Used for fluent assertions on execution results and file content.

## Usage Notes

### Execution Requirements
*   **Environment**: The host machine must have `python3` installed and available in the system `PATH`.
*   **File Paths**: When constructing Python scripts for the `PythonExecutor`, ensure that paths are handled carefully. The tests demonstrate using `.replace("\\", "\\\\")` to ensure compatibility with Windows-style path separators when passed into Python strings.

### Implementation Rationale
*   **Isolation**: By using `@TempDir`, each test case is isolated, preventing file collisions and ensuring that the `workDir` is cleaned up automatically after each test execution.
*   **Resilience Testing**: The `scriptFailureDoesNotPreventOutputCollection` test is specifically designed to ensure that the worker's output collection logic is decoupled from the success state of the execution script, which is crucial for debugging failed tasks where partial results may still be valuable.

### Common Pitfalls
*   **Path Escaping**: Since the `PythonExecutor` often runs in a sub-shell or a different process, failing to properly escape backslashes in file paths on Windows will lead to `SyntaxError` or `FileNotFoundError` within the Python script.
*   **Artifact Naming**: Ensure that the `OutputArtifact` names provided to `collectOutputs` match the actual filenames written by the script; otherwise, the collection service will fail to locate the files.
*   **Hotspot Warning**: Because this file is a hotspot, any changes to `ArtifactService` or `PythonExecutor` should be immediately validated against this test suite to prevent breaking the core worker pipeline.
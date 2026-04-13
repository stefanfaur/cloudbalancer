# File: worker/src/main/java/com/cloudbalancer/worker/service/ArtifactService.java

## Overview

The `ArtifactService` is a Spring-managed service within the `cloudbalancer` worker module responsible for the lifecycle management of task-related files. It handles the staging of input artifacts into a local working directory and the collection/uploading of output artifacts back to the central dispatcher.

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. It is a high-risk area for bugs, particularly regarding file system security and network I/O.

## Public API

### `ArtifactService(long maxSizeBytes, String dispatcherUrl)`
Constructor used for dependency injection.
*   `maxSizeBytes`: Configured via `cloudbalancer.worker.artifacts.max-size-bytes` (default: 100MB).
*   `dispatcherUrl`: Configured via `cloudbalancer.worker.artifacts.dispatcher-url`.

### `void stageInputs(List<InputArtifact> inputs, Path workDir)`
Stages input artifacts into the provided `workDir`.
*   **INLINE**: Decodes Base64 data and writes to disk.
*   **HTTP**: Downloads content from a remote URL.
*   **OBJECT_STORAGE**: Currently throws `UnsupportedOperationException`.

### `List<CollectedArtifact> collectOutputs(List<OutputArtifact> outputs, Path workDir)`
Scans the `workDir` for files specified in the `outputs` list. Returns a list of `CollectedArtifact` objects. Files that do not exist on disk are skipped with a logged warning.

### `void uploadArtifacts(UUID taskId, List<CollectedArtifact> artifacts)`
Transmits collected artifacts to the dispatcher via HTTP POST.
*   **Validation**: Skips files exceeding `maxSizeBytes`.
*   **Endpoint**: `POST {dispatcherUrl}/internal/tasks/{taskId}/artifacts/{artifactName}`.

## Dependencies

*   **Spring Framework**: Uses `@Service` and `@Value` for configuration.
*   **Java HTTP Client**: Uses `java.net.http.HttpClient` for all network operations.
*   **Common Models**: Depends on `com.cloudbalancer.common.model.TaskIO` for `InputArtifact` and `OutputArtifact` definitions.
*   **NIO.2**: Uses `java.nio.file.Files` and `Path` for secure file system operations.

## Usage Notes

### Security: Path Traversal Prevention
The service utilizes a `safeResolve` method to prevent path traversal attacks. When resolving artifact paths against the `workDir`, it verifies that the resulting path remains within the intended directory. If an artifact name attempts to escape the `workDir` (e.g., using `../`), an `IllegalArgumentException` is thrown.

### Error Handling & Edge Cases
*   **Size Limits**: Both `stageInline` and `stageHttp` enforce the `maxSizeBytes` limit. If an artifact exceeds this limit, an `IOException` is thrown during staging, or the file is skipped during upload.
*   **Missing Files**: `collectOutputs` is designed to be non-fatal; if an expected output file is missing, it logs a warning rather than failing the entire task.
*   **Network Failures**: `uploadArtifacts` checks for HTTP 2xx status codes. Non-2xx responses are logged as errors, but the loop continues to attempt uploading remaining artifacts.

### Typical Workflow Example
1.  **Staging**: Call `stageInputs` before executing the task logic to ensure all required files are present in the `workDir`.
2.  **Execution**: Run the task logic, generating files in `workDir`.
3.  **Collection**: Call `collectOutputs` to identify which generated files should be persisted.
4.  **Upload**: Call `uploadArtifacts` to push the results back to the dispatcher.

```java
// Example usage snippet
artifactService.stageInputs(task.getInputs(), workDir);
// ... execute task ...
List<CollectedArtifact> outputs = artifactService.collectOutputs(task.getOutputs(), workDir);
artifactService.uploadArtifacts(task.getId(), outputs);
```
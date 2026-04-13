# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/ArtifactStorageService.java

## Overview

`ArtifactStorageService` is a Spring `@Service` component responsible for the persistent storage and retrieval of task-related artifacts within the `dispatcher` module. It manages file system operations for storing input/output files associated with specific task IDs, ensuring that files are stored within a designated base directory and enforcing size constraints.

## Public API

### `ArtifactStorageService(String basePath, long maxSizeBytes)`
Constructor that initializes the service with configuration properties:
*   `basePath`: The root directory for artifact storage (default: `/tmp/cloudbalancer/artifacts`).
*   `maxSizeBytes`: The maximum allowed size for an individual artifact in bytes (default: `104857600` / 100MB).

### `void store(UUID taskId, String name, InputStream data, long contentLength)`
Stores an artifact stream associated with a specific task.
*   **Parameters**:
    *   `taskId`: The unique identifier of the task.
    *   `name`: The filename of the artifact.
    *   `data`: The input stream containing the file content.
    *   `contentLength`: The size of the data in bytes.
*   **Throws**: `IllegalArgumentException` if the `contentLength` exceeds `maxSizeBytes` or if the filename is invalid (path traversal attempt). `IOException` if file system operations fail.

### `Optional<Path> retrieve(UUID taskId, String name)`
Retrieves the file path for a stored artifact.
*   **Parameters**:
    *   `taskId`: The unique identifier of the task.
    *   `name`: The filename of the artifact.
*   **Returns**: An `Optional<Path>` containing the file path if it exists, otherwise `Optional.empty()`.

## Dependencies

*   **Java NIO (`java.nio.file`)**: Used for robust file system manipulation and path resolution.
*   **Spring Framework**: Utilizes `@Service` for component scanning and `@Value` for externalized configuration.
*   **SLF4J**: Used for logging storage operations.

## Usage Notes

*   **Security**: The service implements a `safeResolve` method to prevent path traversal attacks. It ensures that the resolved path of an artifact remains within the task-specific directory. Any attempt to access files outside the base directory will result in an `IllegalArgumentException`.
*   **Configuration**: The service relies on Spring environment properties. You can override the defaults in your `application.properties` or `application.yml`:
    *   `cloudbalancer.dispatcher.artifact-base-path`: Set the root storage directory.
    *   `cloudbalancer.dispatcher.artifact-max-size-bytes`: Set the maximum file size limit.
*   **Directory Structure**: Artifacts are organized by `taskId` subdirectories within the configured `basePath` (e.g., `{basePath}/{taskId}/{filename}`).
*   **Error Handling**: Callers should handle `IOException` when calling `store` and check the `Optional` return type when calling `retrieve`.
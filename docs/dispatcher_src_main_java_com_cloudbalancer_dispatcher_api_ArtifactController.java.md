# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/ArtifactController.java

## Overview

The `ArtifactController` is a Spring `@RestController` responsible for managing the lifecycle of task-related artifacts within the `cloudbalancer` system. It provides endpoints for both internal system communication (artifact uploads from workers) and external client access (artifact downloads).

The controller acts as a bridge between HTTP requests and the `ArtifactStorageService`, abstracting the underlying file system operations required to store and retrieve task artifacts.

## Public API

### `ArtifactController(ArtifactStorageService artifactStorageService)`
Constructor for dependency injection of the `ArtifactStorageService`.

### `uploadArtifact`
*   **Endpoint**: `POST /internal/tasks/{taskId}/artifacts/{name}`
*   **Description**: Receives binary data from a worker and stores it as an artifact associated with a specific task.
*   **Parameters**:
    *   `taskId` (UUID): The unique identifier of the task.
    *   `name` (String): The filename or identifier for the artifact.
    *   `request` (HttpServletRequest): Used to access the input stream and content length.
*   **Returns**: `ResponseEntity<Void>` (200 OK on success).

### `downloadArtifact`
*   **Endpoint**: `GET /api/tasks/{taskId}/artifacts/{name}`
*   **Description**: Retrieves a stored artifact for a given task and returns it as a downloadable resource.
*   **Parameters**:
    *   `taskId` (UUID): The unique identifier of the task.
    *   `name` (String): The filename or identifier for the artifact.
*   **Returns**: `ResponseEntity<Resource>` containing the file content, or 404 Not Found if the artifact does not exist.

## Dependencies

*   `com.cloudbalancer.dispatcher.service.ArtifactStorageService`: Service layer responsible for the physical storage and retrieval logic of artifacts.
*   `jakarta.servlet.http.HttpServletRequest`: Used for reading raw input streams during file uploads.
*   `org.springframework.core.io.Resource` / `UrlResource`: Used to represent the file system artifact as a streamable Spring resource.
*   `org.springframework.http.ResponseEntity`: Used for constructing standardized HTTP responses.

## Usage Notes

*   **Authentication**: The `uploadArtifact` endpoint is intended for internal use by worker nodes and is currently unauthenticated. The `downloadArtifact` endpoint is intended for public/client access and should be secured via the application's security configuration.
*   **Content Types**: The `downloadArtifact` method attempts to detect the MIME type of the file using `Files.probeContentType`. If detection fails, it defaults to `application/octet-stream`.
*   **Error Handling**: If an artifact is requested that does not exist, the controller returns a `404 Not Found` status. Internal server errors during file retrieval result in a `500 Internal Server Error`.
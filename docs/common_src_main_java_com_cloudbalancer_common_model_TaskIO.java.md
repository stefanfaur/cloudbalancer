# File: common/src/main/java/com/cloudbalancer/common/model/TaskIO.java

## Overview

`TaskIO` is a core data model within the `com.cloudbalancer.common.model` package. It serves as a container for managing the input and output artifacts associated with a specific task in the CloudBalancer system. By utilizing Java `record` types, it provides an immutable and concise structure for defining task requirements and results.

## Public API

### `TaskIO` (Record)
The primary container for task-related artifacts.

*   **`inputs()`**: Returns a `List<InputArtifact>` representing the required inputs for the task.
*   **`outputs()`**: Returns a `List<OutputArtifact>` representing the expected outputs of the task.

### `TaskIO.InputArtifact` (Record)
Defines an input requirement for a task.
*   **`name()`**: The identifier for the input.
*   **`location()`**: The URI or path where the input artifact is located.
*   **`source()`**: The `ArtifactSource` enum indicating the origin type of the artifact.

### `TaskIO.OutputArtifact` (Record)
Defines an output produced by a task.
*   **`name()`**: The identifier for the output.
*   **`path()`**: The destination path or reference for the output artifact.

### `ArtifactSource` (Enum)
Defines the supported origins for input artifacts:
*   **`HTTP`**: Artifact is retrieved via HTTP/HTTPS.
*   **`OBJECT_STORAGE`**: Artifact is retrieved from a cloud object storage service.
*   **`INLINE`**: Artifact data is provided directly within the task definition.

### `none()` (Static Method)
*   **Signature**: `public static TaskIO none()`
*   **Description**: A factory method that returns a `TaskIO` instance with empty input and output lists. Useful for initializing tasks that require no I/O operations.

## Dependencies

*   `java.util.List`: Used for managing collections of input and output artifacts.

## Usage Notes

*   **Immutability**: Since `TaskIO` and its nested components are defined as Java `records`, they are immutable. Ensure that all artifact lists are fully defined at the time of object construction.
*   **Initialization**: Use the `TaskIO.none()` method when creating a task that does not involve external data dependencies or generated artifacts to avoid null pointer issues or unnecessary list allocations.
*   **Artifact Resolution**: When processing `InputArtifact`, the `source` field should be used to determine the appropriate downloader or handler implementation required to fetch the data from the specified `location`.
# File: worker/src/test/java/com/cloudbalancer/worker/service/ArtifactServiceTest.java

## Overview

`ArtifactServiceTest` is a JUnit 5 test suite designed to validate the functionality of the `ArtifactService` within the worker module. The service is responsible for staging input artifacts into the local workspace and collecting output artifacts generated during task execution. The tests ensure that the service correctly handles various artifact sources, manages file I/O operations, and gracefully handles edge cases such as missing files or empty input lists.

## Public API

The `ArtifactServiceTest` class contains the following test methods:

*   **`stageInlineArtifactWritesDecodedContent`**: Verifies that `ArtifactService` correctly decodes Base64-encoded inline artifacts and writes them to the designated workspace directory.
*   **`stageObjectStorageThrowsUnsupported`**: Validates that the service throws an `UnsupportedOperationException` when attempting to stage artifacts from `OBJECT_STORAGE`, confirming that this feature is not yet implemented.
*   **`collectOutputsReturnsExistingFiles`**: Ensures that the service correctly identifies and collects output artifacts that exist within the workspace.
*   **`collectOutputsSkipsMissingFiles`**: Verifies that the service ignores output definitions if the corresponding file is not present in the workspace, preventing runtime errors.
*   **`stageEmptyListDoesNothing`**: Confirms that passing an empty list of artifacts to the staging process results in a no-op, ensuring system stability.

## Dependencies

The test suite relies on the following external libraries and internal components:

*   **JUnit 5 (Jupiter)**: Provides the testing framework (`@Test`, `@TempDir`).
*   **AssertJ**: Used for fluent assertions (`assertThat`, `assertThatThrownBy`).
*   **Java NIO**: Used for file system operations (`Files`, `Path`).
*   **Common Models**: `InputArtifact`, `OutputArtifact`, and `ArtifactSource` from `com.cloudbalancer.common.model.TaskIO`.
*   **ArtifactService**: The service under test (located in `com.cloudbalancer.worker.service`).

## Usage Notes

*   **Temporary Directories**: The tests utilize the `@TempDir` annotation to inject a temporary workspace directory. This ensures that file system operations are isolated and cleaned up automatically after each test execution.
*   **Base64 Encoding**: When testing `stageInlineArtifactWritesDecodedContent`, ensure that the input content is correctly encoded using `java.util.Base64` to match the expected service behavior.
*   **Unsupported Features**: Note that `OBJECT_STORAGE` source types are currently unsupported and will trigger an `UnsupportedOperationException`. This is intentional and should be accounted for in production logic.
*   **Execution Environment**: These tests require a standard Java environment with access to the `com.cloudbalancer.common` library. No external network connections are required as the `ArtifactService` is instantiated with local configuration parameters.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/ArtifactStorageServiceTest.java

## Overview

`ArtifactStorageServiceTest` is a JUnit 5 test suite that validates the core functionality and security constraints of the `ArtifactStorageService`. The tests ensure that the service correctly handles the lifecycle of task artifacts, including storage, retrieval, and enforcement of security policies such as path traversal prevention and size limits.

## Public API

The test class validates the following behaviors of the `ArtifactStorageService`:

*   **`storeAndRetrieveArtifact`**: Verifies that an artifact can be successfully stored for a given `UUID` task ID and retrieved with its original content intact.
*   **`retrieveNonExistentReturnsEmpty`**: Confirms that attempting to retrieve an artifact that does not exist returns an empty result rather than throwing an exception.
*   **`storeRejectsPathTraversal`**: Ensures that the service prevents directory traversal attacks by rejecting filenames containing path traversal sequences (e.g., `../`).
*   **`retrieveRejectsPathTraversal`**: Ensures that the service prevents directory traversal during retrieval operations.
*   **`storeRejectsOversizedArtifact`**: Validates that the service enforces maximum file size limits by rejecting artifacts that exceed the configured capacity.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management and assertions.
*   **AssertJ**: Used for fluent, readable assertions (`assertThat`, `assertThatThrownBy`).
*   **Java NIO**: Utilized via `@TempDir` to manage isolated, temporary file system environments for each test execution.
*   **`ArtifactStorageService`**: The system under test (SUT) located in the `dispatcher` module.

## Usage Notes

*   **Isolated Testing**: The test suite uses the `@TempDir` annotation, which automatically creates and cleans up a temporary directory for each test method. This ensures that file system state does not leak between test runs.
*   **Security Testing**: The tests explicitly verify security boundaries. When implementing or modifying the `ArtifactStorageService`, ensure that any changes to path normalization or size validation logic are reflected in these test cases.
*   **Configuration**: Tests initialize the `ArtifactStorageService` with specific storage limits (e.g., 104,857,600 bytes or 10 bytes), demonstrating how the service should be instantiated in production environments.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/ArtifactRoundTripTest.java

## Overview

`ArtifactRoundTripTest` is a critical integration test suite for the `ArtifactStorageService`. It validates the end-to-end lifecycle of artifact storage, ensuring that data written to the service can be retrieved with full integrity. 

The suite operates without a Spring context, utilizing direct instantiation and JUnit 5's `@TempDir` to manage filesystem resources. It covers essential storage behaviors, including content encoding, multi-file handling, cross-task isolation, and overwrite logic.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It represents a high-risk area for regressions in the storage layer; any modifications to the `ArtifactStorageService` should be verified against these test cases.

## Public API

The test class does not expose a public API but provides the following test methods:

*   `storeAndRetrieveRoundTrip`: Verifies basic write/read functionality with special character support.
*   `multipleArtifactsPerTaskIsolated`: Ensures multiple files can be stored under the same task ID without collision.
*   `artifactsIsolatedBetweenTasks`: Confirms that identical filenames in different tasks remain isolated.
*   `storeOverwritesExistingArtifact`: Validates that subsequent writes to the same task/filename pair correctly replace the previous content.
*   `retrieveNonExistentTaskReturnsEmpty`: Confirms graceful handling of missing artifacts.
*   `binaryContentPreservedExactly`: Validates byte-for-byte integrity for non-text binary data (0x00-0xFF).

## Dependencies

*   **JUnit 5**: Used for test lifecycle management and assertions.
*   **AssertJ**: Provides fluent assertions (`assertThat`).
*   **Java NIO**: Used for filesystem operations and path management.
*   **ArtifactStorageService**: The system-under-test (SUT) located in the `dispatcher` module.

## Usage Notes

### Implementation Rationale
The tests are designed to be environment-agnostic by using `@TempDir`. This ensures that tests do not rely on pre-existing directory structures or environment variables, making them suitable for CI/CD pipelines.

### Edge Cases and Pitfalls
*   **Binary Integrity**: The `binaryContentPreservedExactly` test is crucial. It ensures that the service does not perform any character encoding/decoding transformations that might corrupt binary blobs.
*   **Isolation**: The `artifactsIsolatedBetweenTasks` test is the primary safeguard against directory traversal or namespace collision bugs. Ensure that any changes to the storage path generation logic maintain this strict separation.
*   **Overwrite Behavior**: The `storeOverwritesExistingArtifact` test confirms that the service does not throw exceptions when updating existing files, which is a common requirement for task-based artifact updates.

### Testing Strategy
To add a new test case for the storage service:
1.  Inject a `@TempDir Path baseDir` into your test method.
2.  Instantiate `ArtifactStorageService` with `baseDir.toString()` and a defined capacity.
3.  Perform the operation (store/retrieve).
4.  Use `AssertJ` to verify the `Optional` result and the content of the retrieved file using `Files.readAllBytes()`.

```java
// Example: Testing a new storage scenario
@Test
void customScenario(@TempDir Path baseDir) throws Exception {
    var service = new ArtifactStorageService(baseDir.toString(), 1024);
    UUID taskId = UUID.randomUUID();
    // ... perform operations
    assertThat(service.retrieve(taskId, "file.txt")).isPresent();
}
```
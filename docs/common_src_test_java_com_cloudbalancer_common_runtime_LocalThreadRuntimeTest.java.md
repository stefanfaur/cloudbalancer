# File: common/src/test/java/com/cloudbalancer/common/runtime/LocalThreadRuntimeTest.java

## Overview

`LocalThreadRuntimeTest` is a JUnit 5 test suite designed to verify the lifecycle management and state consistency of the `LocalThreadRuntime` component. It ensures that the runtime correctly handles the registration, tracking, and termination of worker threads within the local environment.

## Public API

### `LocalThreadRuntimeTest`
The test class validates the following behaviors of `LocalThreadRuntime`:

*   **`startsWithNoWorkers()`**: Verifies that a newly instantiated `LocalThreadRuntime` contains no active workers.
*   **`startWorkerRegistersAndReturnsTrue()`**: Confirms that providing a valid `WorkerConfig` successfully registers the worker, returns a success status, and allows retrieval of the worker's metadata.
*   **`stopWorkerRemovesIt()`**: Validates that calling `stopWorker` correctly removes the specified worker from the internal registry.
*   **`getWorkerInfoReturnsNullForUnknownWorker()`**: Ensures that querying for a non-existent worker ID returns `null` rather than throwing an exception.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test execution and lifecycle annotations.
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **`com.cloudbalancer.common.model.ExecutorType`**: Used to define the execution capabilities of test workers.
*   **`com.cloudbalancer.common.runtime.LocalThreadRuntime`**: The system under test (SUT).
*   **`com.cloudbalancer.common.runtime.WorkerConfig`**: Data structure required for initializing worker instances.

## Usage Notes

*   These tests are intended for unit testing the local runtime implementation. They do not require external network connections or complex infrastructure, as they rely on in-memory state management.
*   The tests assume that `LocalThreadRuntime` is thread-safe or used in a single-threaded test context, as they manipulate the internal state of the runtime directly.
*   When extending these tests, ensure that `WorkerConfig` objects are properly populated, as the runtime relies on these configurations to identify and manage workers.
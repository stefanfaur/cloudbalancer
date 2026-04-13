# File: common/src/test/java/com/cloudbalancer/common/executor/DockerExecutorTest.java

## Overview

`DockerExecutorTest` is a critical JUnit 5 test suite that validates the `DockerExecutor` component, which is responsible for orchestrating task execution within isolated Docker containers. 

**Status**: This file is a **HOTSPOT**. It ranks in the top 25% for both change frequency and complexity. As the primary integration point between the `cloudbalancer` system and the Docker daemon, changes here carry a high risk of regression. It is essential to ensure that any modifications to container lifecycle management, resource constraints, or security policies are thoroughly verified against this suite.

The suite leverages [Testcontainers](https://www.testcontainers.org/) to spin up a transient `docker:dind` (Docker-in-Docker) container, providing a real, isolated environment for testing container creation, execution, resource limiting, and cleanup.

## Public API

The `DockerExecutorTest` class does not expose a public API for production use; it is a test-only class. However, it exercises the following key methods of the `DockerExecutor` class:

*   `validate(Map<String, Object> spec)`: Ensures the executor correctly rejects or accepts task specifications based on required fields (e.g., `image`).
*   `execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context)`: The primary entry point for running tasks. Validates container lifecycle, output streaming, and error handling.
*   `cancel(ExecutionHandle handle)`: Validates the ability to interrupt running containers and ensure proper cleanup of resources.
*   `estimateResources(Map<String, Object> spec)`: Verifies that the executor provides sensible default resource requirements for tasks.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management (`@BeforeAll`, `@Test`).
*   **Testcontainers**: Provides the `GenericContainer` infrastructure to run a real Docker daemon for integration testing.
*   **Docker Java API**: The underlying client library used by `DockerExecutor` to communicate with the Docker daemon.
*   **AssertJ**: Used for fluent, readable assertions.
*   **CloudBalancer Models**: Relies on internal domain models (`ExecutorCapabilities`, `ExecutorType`, `SecurityLevel`, `TaskContext`, `ResourceAllocation`).

## Usage Notes

### Running the Tests
Because this suite requires a Docker environment, it is annotated with `@Testcontainers(disabledWithoutDocker = true)`. 
*   **Prerequisite**: Ensure a Docker daemon is running on the host machine.
*   **Execution**: Run via standard Maven/Gradle commands: `mvn test -Dtest=DockerExecutorTest`.

### Key Testing Patterns
1.  **Container Cleanup**: Every test that executes a container must verify that the container is removed after the task completes. This is checked by querying the Docker daemon for containers with the `cloudbalancer.task-id` label.
2.  **Resource Constraints**: The suite includes specific tests for memory limits (`executeWithMemoryLimitKillsOOMContainer`) and read-only filesystems (`executeWithReadOnlyRootfsFailsOnWrite`). These tests rely on specific shell commands to trigger failure states.
3.  **Asynchronous Execution**: The `cancel` test demonstrates how to handle long-running tasks by using `CompletableFuture` to run the executor in a non-blocking manner, allowing the test thread to trigger a cancellation.

### Potential Pitfalls
*   **DIND Overhead**: The `docker:dind` container is resource-intensive. If tests are failing intermittently, check if the CI environment has sufficient memory and CPU to support nested Docker execution.
*   **Label Collisions**: The cleanup logic relies on `cloudbalancer.task-id` labels. Ensure that the `TaskContext` used in tests generates unique IDs to prevent cross-test interference.
*   **Network Flakiness**: Tests involving network-disabled containers rely on the assumption that the `alpine` image is available locally or can be pulled quickly. Ensure the test environment has appropriate registry access.
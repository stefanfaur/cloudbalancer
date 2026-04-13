# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AdminControllerWorkerTagsTest.java

## Overview

`AdminControllerWorkerTagsTest` is an integration test suite designed to verify the authorization and functional logic of the worker tag management API endpoints. It ensures that only users with the `ADMIN` role can modify worker tags and validates the system's response to valid and invalid requests.

The test suite utilizes `MockMvc` to simulate HTTP requests against the `AdminController` and leverages `TestContainersConfig` to provide a controlled environment for testing against the underlying infrastructure services.

## Public API

The test class does not expose a public API as it is a test suite. However, it validates the following endpoint:

- `PUT /api/admin/workers/{workerId}/tags`: Updates the tags associated with a specific worker.

## Dependencies

- **Spring Boot Test**: Provides the testing framework and context loading (`@SpringBootTest`, `@AutoConfigureMockMvc`).
- **JUnit 5**: The underlying testing framework.
- **MockMvc**: Used for performing simulated HTTP requests.
- **CloudBalancer Common Models**: Utilizes `WorkerHealthState`, `WorkerCapabilities`, and `Role` for setting up test data.
- **WorkerRegistryService**: Used to register test workers in the registry before executing tests.
- **JwtService**: Used to generate valid authentication tokens for `ADMIN` and `OPERATOR` roles.
- **TestContainersConfig**: Provides necessary containerized dependencies for the integration tests.

## Usage Notes

- **Authentication**: The tests verify role-based access control (RBAC). Tests are performed using generated JWT tokens for both `ADMIN` and `OPERATOR` roles to ensure that unauthorized roles (e.g., `OPERATOR`) are correctly restricted from performing administrative actions.
- **Test Setup**: The `setUp()` method initializes the environment by registering a worker named `tag-test-worker` with initial capabilities and tags. This ensures a consistent state for each test execution.
- **Assertions**:
    - Valid updates by an `ADMIN` are expected to return `200 OK`.
    - Requests targeting non-existent workers are expected to return `404 Not Found`.
    - Requests made by an `OPERATOR` are expected to return `403 Forbidden`.
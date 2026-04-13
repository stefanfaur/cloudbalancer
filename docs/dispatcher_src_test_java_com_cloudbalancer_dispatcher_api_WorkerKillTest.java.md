# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/WorkerKillTest.java

## Overview

`WorkerKillTest` is an integration test suite for the `dispatcher` module, specifically validating the administrative API endpoint for terminating workers. It ensures that the system correctly handles worker lifecycle state transitions (e.g., moving a worker to a `STOPPING` state) and enforces role-based access control (RBAC) for administrative operations.

The tests utilize `MockMvc` to simulate HTTP requests against the dispatcher's REST API and leverage `TestContainersConfig` to provide a realistic environment for testing service interactions.

## Public API

The class provides the following test scenarios:

*   **`killWorker_asAdmin_existingWorker_returns204`**: Verifies that an authenticated `ADMIN` can successfully initiate the termination of an existing, healthy worker, resulting in a `204 No Content` status and updating the worker state to `STOPPING`.
*   **`killWorker_notFound_returns404`**: Confirms that attempting to kill a non-existent worker returns a `404 Not Found` status.
*   **`killWorker_alreadyStopping_returns409`**: Ensures that attempting to kill a worker that is already in the process of stopping returns a `409 Conflict` status.
*   **`killWorker_asOperator_returns403`**: Validates that users with the `OPERATOR` role are denied access to the kill endpoint, receiving a `403 Forbidden` status.

## Dependencies

*   **Spring Boot Test**: Provides the testing framework and `MockMvc` for API integration testing.
*   **JUnit 5**: Used as the primary testing engine.
*   **`WorkerRegistryService`**: Used to register and verify the state of workers during test execution.
*   **`JwtService`**: Used to generate valid authentication tokens for `ADMIN` and `OPERATOR` roles.
*   **`TestContainersConfig`**: Provides necessary infrastructure dependencies for the integration tests.

## Usage Notes

*   **Authentication**: The tests utilize helper methods `adminToken()` and `operatorToken()` to generate JWTs. These tokens must be included in the `Authorization` header as `Bearer <token>` for all requests to the `/api/admin/workers/` endpoint.
*   **State Management**: Tests rely on the `WorkerRegistryService` to set up the initial state of the system (e.g., registering a worker with specific `WorkerCapabilities`).
*   **Environment**: Because this is an integration test, it requires a configured Spring context. Ensure that the `TestContainersConfig` is correctly configured to provide the necessary backing services (e.g., databases or message brokers) required by the `dispatcher` module.
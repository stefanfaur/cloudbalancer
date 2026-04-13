# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/TaskController.java

## Overview

The `TaskController` is a Spring `@RestController` located in the `dispatcher` module. It serves as the primary HTTP interface for managing task lifecycles within the CloudBalancer system. The controller exposes endpoints for task submission, retrieval, paginated listing, and bulk operations such as cancellation, retrying, and priority adjustment.

It delegates business logic to the `TaskService`, ensuring a clean separation between the API layer and the underlying task orchestration engine.

## Public API

### Endpoints

*   **`POST /api/tasks`** (`submitTask`): Submits a new task for execution. Requires a `TaskDescriptor` in the request body. Returns a `TaskEnvelope` with a `201 Created` status.
*   **`GET /api/tasks/{id}`** (`getTask`): Retrieves the current status and details of a specific task by its `UUID`. Returns a `404 Not Found` if the task does not exist.
*   **`GET /api/tasks`** (`listTasks`): Retrieves a paginated list of tasks. Supports filtering by `status`, `priority`, `executorType`, `workerId`, and a timestamp (`since`).
*   **`POST /api/tasks/bulk/cancel`** (`bulkCancel`): Cancels a list of tasks provided in a `BulkTaskRequest`. Returns a list of `BulkResultEntry` objects detailing the outcome for each task.
*   **`POST /api/tasks/bulk/retry`** (`bulkRetry`): Retries a list of tasks provided in a `BulkTaskRequest`. Returns a list of `BulkResultEntry` objects.
*   **`POST /api/tasks/bulk/reprioritize`** (`bulkReprioritize`): Updates the priority for a list of tasks. Requires a `BulkReprioritizeRequest` containing the task IDs and the new `Priority` level.

## Dependencies

*   **`com.cloudbalancer.common.model.*`**: Provides core domain models (e.g., `TaskState`, `Priority`, `ExecutorType`).
*   **`com.cloudbalancer.dispatcher.api.dto.*`**: Provides Data Transfer Objects for request/response handling (e.g., `TaskDescriptor`, `BulkTaskRequest`, `BulkReprioritizeRequest`).
*   **`com.cloudbalancer.dispatcher.service.TaskService`**: The service layer component responsible for executing business logic and interacting with the task persistence layer.
*   **Spring Framework**: Utilizes `org.springframework.web.bind.annotation` for routing and `org.springframework.http` for response management.

## Usage Notes

*   **Validation**: The `submitTask` method performs a basic check to ensure the `executorType` is present in the `TaskDescriptor`. If missing, it returns a `400 Bad Request`.
*   **Pagination**: The `listTasks` endpoint defaults to an `offset` of 0 and a `limit` of 50. These can be overridden via query parameters.
*   **Bulk Operations**: All bulk operations return a `List<BulkResultEntry>`, which should be inspected by the client to determine the success or failure of individual task updates.
*   **Dependency Injection**: This controller uses constructor injection for `TaskService`, making it suitable for unit testing with mocked service dependencies.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/LogController.java

## Overview

`LogController` is a Spring `@RestController` responsible for exposing task execution logs via a RESTful API. It provides a mechanism for clients to retrieve the standard output (`stdout`) and standard error (`stderr`) logs associated with a specific task identified by its unique identifier.

## Public API

### `LogController`
The controller is mapped to the `/api/tasks` base path.

#### `LogController(TaskRepository taskRepository)`
Constructor-based dependency injection for the `TaskRepository`.

#### `ResponseEntity<TaskLogsResponse> getTaskLogs(@PathVariable UUID id)`
Retrieves the logs for a specific task.
- **Endpoint**: `GET /api/tasks/{id}/logs`
- **Parameters**: 
    - `id` (UUID): The unique identifier of the task.
- **Returns**: 
    - `200 OK` with a `TaskLogsResponse` body containing the `stdout` and `stderr` if the task is found.
    - `404 Not Found` if no task exists with the provided ID.

## Dependencies

- `com.cloudbalancer.dispatcher.api.dto.TaskLogsResponse`: DTO used to structure the log response.
- `com.cloudbalancer.dispatcher.persistence.TaskRepository`: Persistence layer interface used to query task data from the database.
- `org.springframework.http.ResponseEntity`: Used for constructing HTTP responses.
- `org.springframework.web.bind.annotation`: Provides Spring MVC annotations for REST endpoints.
- `java.util.UUID`: Used for identifying tasks.

## Usage Notes

- This controller provides a snapshot of the logs stored in the persistence layer. For real-time log streaming, refer to `LogStreamWebSocketHandler` and `LogStreamListener`.
- The controller assumes that the `TaskRepository` correctly maps the persistence entity fields `lastStdout` and `lastStderr` to the `TaskLogsResponse` object.
- Ensure that the `UUID` passed in the path is a valid format, otherwise, the framework will return a `400 Bad Request`.
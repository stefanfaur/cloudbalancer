# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/TaskPageResponse.java

## Overview

The `TaskPageResponse` class is a Java `record` used as a Data Transfer Object (DTO) to encapsulate a paginated collection of task envelopes. It is designed to provide a standardized response format for API endpoints that return lists of tasks, including metadata required for client-side pagination.

## Public API

### `TaskPageResponse`

```java
public record TaskPageResponse(
    List<TaskEnvelope> tasks, 
    long total, 
    int offset, 
    int limit
) {}
```

#### Components
*   **`tasks`**: A `List<TaskEnvelope>` containing the specific subset of tasks for the requested page.
*   **`total`**: A `long` representing the total number of tasks available across all pages.
*   **`offset`**: An `int` indicating the starting index of the current page.
*   **`limit`**: An `int` representing the maximum number of tasks requested for this page.

## Dependencies

*   `com.cloudbalancer.common.model.TaskEnvelope`: The core model representing the task data structure being paginated.
*   `java.util.List`: Standard Java collection interface used to hold the task data.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the contents of the response cannot be modified.
*   **Pagination Logic**: This DTO is intended to be used by service layers to return consistent pagination metadata to the API consumer. Ensure that the `total` field reflects the count of the entire dataset, not just the size of the `tasks` list.
*   **Serialization**: Being a standard record, it is compatible with common JSON serialization libraries (such as Jackson) used in Spring Boot or other Java-based web frameworks.
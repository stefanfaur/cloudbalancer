# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/TaskControllerPaginationTest.java

## Overview

`TaskControllerPaginationTest` is a comprehensive integration test suite designed to validate the pagination, filtering, and bulk operation capabilities of the `TaskController` within the CloudBalancer dispatcher module. 

**Note:** This file is a **HOTSPOT** (top 25% for change frequency and complexity). It serves as a critical verification point for task lifecycle management. Any modifications to task state transitions or API query parameters should be thoroughly tested against this suite to prevent regressions in task scheduling and orchestration.

## Public API

The test class validates the following REST API endpoints:

*   **`GET /api/tasks`**: Validates pagination (offset/limit) and status-based filtering.
*   **`POST /api/tasks/bulk/cancel`**: Validates the ability to transition multiple tasks to a `CANCELLED` state.
*   **`POST /api/tasks/bulk/retry`**: Validates the ability to re-queue tasks that have reached a `FAILED` state.

## Dependencies

This test suite relies on the following core infrastructure:

*   **Spring Boot Test**: Uses `@SpringBootTest` and `@AutoConfigureMockMvc` for full context integration.
*   **TestContainers**: Uses `TestContainersConfig` to ensure a consistent environment for database and service interactions.
*   **Internal Services**:
    *   `TaskService`: Used for direct task submission and state manipulation.
    *   `UserService` & `JwtService`: Used for managing authentication and generating valid `OPERATOR` role tokens required for API access.
*   **MockMvc**: Used for simulating HTTP requests and asserting response status and JSON structure.

## Usage Notes

### Implementation Rationale
The tests utilize `submitSimulated()` to generate controlled task workloads. This helper method creates tasks with `ExecutorType.SIMULATED`, ensuring that tests do not rely on external worker availability, which keeps the test suite deterministic and fast.

### Key Test Scenarios
1.  **Pagination**: Verifies that the `offset` and `limit` parameters correctly slice the result set returned by the `TaskService`.
2.  **Filtering**: Ensures that the `status` query parameter correctly filters the task list (e.g., retrieving only `QUEUED` tasks).
3.  **Bulk Operations**:
    *   **Cancel**: Demonstrates the workflow for terminating multiple tasks simultaneously.
    *   **Retry**: Requires a multi-step state transition (e.g., `ASSIGNED` -> `RUNNING` -> `FAILED`) before the retry endpoint can successfully re-queue the task.

### Potential Pitfalls
*   **State Dependency**: The `bulkRetryRequeuesFailedTasks` test relies on manual state transitions via `taskService.updateTask()`. If the internal state machine logic changes, these manual transitions may become invalid or trigger validation errors.
*   **Token Expiration**: The `operatorToken` is generated in `setUp()`. While sufficient for current tests, ensure that any future long-running tests account for token expiration if the `JwtService` configuration is tightened.
*   **Hotspot Risk**: Because this file is a high-activity area, ensure that any changes to the `TaskController` DTOs (like `BulkTaskRequest`) are reflected here immediately to prevent build failures.

### Example: Adding a New Pagination Test
To test a new filter parameter (e.g., `priority`), follow this pattern:

```java
@Test
void filterByPriorityReturnsMatchingTasks() throws Exception {
    submitSimulated(); // Default is NORMAL
    
    mvc.perform(get("/api/tasks?priority=NORMAL")
            .header("Authorization", "Bearer " + operatorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[0].priority").value("NORMAL"));
}
```
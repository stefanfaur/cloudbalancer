# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/TaskControllerTest.java

## Overview

`TaskControllerTest` is a comprehensive JUnit 5 test suite designed to validate the REST API endpoints exposed by the `TaskController`. It utilizes Spring Boot's `@WebMvcTest` to isolate the web layer, ensuring that request routing, input validation, security authorization, and response formatting function as expected without requiring a full application context.

**Note:** This file is a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for regressions; changes to the API contract or security filters should be carefully verified against these tests.

## Public API

The test suite validates the following endpoints:

*   **`POST /api/tasks`**: Validates task submission. Ensures that valid `TaskDescriptor` objects result in a `201 Created` status and a valid `TaskEnvelope` response.
*   **`GET /api/tasks/{id}`**: Validates retrieval of a single task by its unique identifier.
*   **`GET /api/tasks`**: Validates paginated listing of tasks.

### Test Methods

*   `submitTaskReturns201WithEnvelope()`: Verifies successful task creation with valid payload.
*   `submitTaskWithNullExecutorTypeReturns400()`: Verifies that malformed payloads (missing required fields) trigger a `400 Bad Request`.
*   `getTaskByIdReturnsTask()`: Verifies that a valid task ID returns the expected task details.
*   `getTaskByNonexistentIdReturns404()`: Verifies that requests for non-existent tasks return a `404 Not Found`.
*   `listTasksReturnsPagedResult()`: Verifies that the task listing endpoint returns correctly structured `TaskPageResponse` objects.

## Dependencies

*   **Spring WebMvcTest**: Provides the `MockMvc` environment for testing controllers.
*   **Mockito**: Used for mocking `TaskService`, `JwtService`, `UserService`, and `RefreshTokenRepository` to isolate the controller logic.
*   **Security**: Integrates `RateLimitFilter` and `RateLimitProperties` via `@Import` to ensure security configurations are correctly applied during tests.
*   **Common Models**: Relies on `com.cloudbalancer.common.model.*` for `TaskDescriptor`, `TaskEnvelope`, and `TaskState` definitions.

## Usage Notes

### Testing Strategy
The suite uses `@WithMockUser` to simulate authenticated users with specific roles (`OPERATOR` for write operations, `VIEWER` for read operations). This is critical for testing Spring Security integration.

### Hotspot Warning
As a high-activity file, any modifications to the `TaskController` request mapping or DTO structures will likely require updates here. When adding new fields to `TaskDescriptor`, ensure that `submitTaskReturns201WithEnvelope` is updated to reflect the new state transitions or validation requirements.

### Example: Adding a new test case
To test a new endpoint or a new validation rule, follow this pattern:

1.  **Mock the Service**: Use `@MockitoBean` to define the expected behavior of the `TaskService`.
2.  **Perform the Request**: Use `MockMvc` to trigger the endpoint.
3.  **Assert the Outcome**: Use `MockMvcResultMatchers` (e.g., `status()`, `jsonPath()`) to verify the response.

```java
@Test
@WithMockUser(roles = "OPERATOR")
void exampleNewTest() throws Exception {
    // 1. Setup mock
    when(taskService.someMethod()).thenReturn(someResult);
    
    // 2. Execute and Assert
    mvc.perform(get("/api/tasks/new-endpoint"))
       .andExpect(status().isOk());
}
```

### Common Pitfalls
*   **JSON Serialization**: Ensure that `JsonUtil` is used for complex objects to maintain consistency with the production serialization configuration.
*   **Security Context**: If a test fails with `401 Unauthorized` or `403 Forbidden`, verify the `@WithMockUser` role matches the security requirements defined in the controller's method-level security annotations.
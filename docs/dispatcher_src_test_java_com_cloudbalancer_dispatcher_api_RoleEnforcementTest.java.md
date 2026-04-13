# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/RoleEnforcementTest.java

## Overview

`RoleEnforcementTest` is a critical integration test suite for the `dispatcher` module, designed to validate the system's Role-Based Access Control (RBAC) implementation. It ensures that API endpoints correctly enforce security policies based on the user's assigned `Role`.

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because this test suite governs the security boundary of the dispatcher, any regressions here could lead to unauthorized access or service disruption. Exercise extreme caution when modifying security policies or the underlying `JwtService`.

## Public API

The class provides a suite of JUnit 5 test methods that simulate various authentication and authorization scenarios:

*   **`viewerCannotSubmitTasks()`**: Verifies that users with the `VIEWER` role are denied permission (403 Forbidden) when attempting to POST to `/api/tasks`.
*   **`operatorCanSubmitTasks()`**: Verifies that users with the `OPERATOR` role are granted permission (201 Created) to submit tasks.
*   **`viewerCanReadTasks()`**: Verifies that users with the `VIEWER` role can successfully access (200 OK) task information.
*   **`unauthenticatedRequestReturns401()`**: Ensures that requests lacking an `Authorization` header are rejected with a 401 Unauthorized status.
*   **`expiredTokenReturns401()`**: Validates that tokens with expired timestamps are rejected by the security filter.
*   **`apiClientCanSubmitTasks()`**: Confirms that the `API_CLIENT` role has sufficient privileges to submit tasks.
*   **`adminCanDoEverything()`**: A comprehensive test ensuring the `ADMIN` role has full access to both read and write operations.

### Helper Methods
*   **`tokenFor(String user, Role role)`**: Generates a valid JWT for testing purposes using the injected `JwtService`.
*   **`taskDescriptorJson()`**: Constructs a valid `TaskDescriptor` object and serializes it to JSON for use in POST requests.

## Dependencies

*   **`MockMvc`**: Used for testing the Spring MVC layer without starting a full HTTP server.
*   **`JwtService`**: Used to generate tokens and simulate authentication states.
*   **`TestContainersConfig`**: Provides the necessary infrastructure (e.g., databases) to support integration tests.
*   **`com.cloudbalancer.common.model`**: Provides the domain models (`TaskDescriptor`, `Role`, etc.) required to construct valid API payloads.

## Usage Notes

### Testing Security Policies
When adding new roles or modifying existing API endpoints, ensure that you update this test suite to reflect the new security requirements. 

### Common Pitfalls
1.  **Token Expiration**: The `expiredTokenReturns401` test creates a local instance of `JwtService` with a zero-second expiration. If the `JwtService` constructor signature changes, this test will fail to compile.
2.  **MockMvc Context**: This test uses `@SpringBootTest` and `@AutoConfigureMockMvc`. This loads the full application context. If tests are running slowly, ensure that the `TestContainersConfig` is efficiently managing resources.
3.  **JSON Serialization**: The `taskDescriptorJson` method relies on `JsonUtil`. If the `TaskDescriptor` model changes, ensure the JSON structure remains compatible with the expected API contract.

### Example: Adding a new Role Test
To test a new role (e.g., `AUDITOR`), add a new test method following the existing pattern:

```java
@Test
void auditorCanOnlyReadTasks() throws Exception {
    mvc.perform(get("/api/tasks")
            .header("Authorization", "Bearer " + tokenFor("auditor", Role.AUDITOR)))
        .andExpect(status().isOk());

    mvc.perform(post("/api/tasks")
            .header("Authorization", "Bearer " + tokenFor("auditor", Role.AUDITOR))
            .contentType(MediaType.APPLICATION_JSON)
            .content(taskDescriptorJson()))
        .andExpect(status().isForbidden());
}
```
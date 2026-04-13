# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AdminControllerTest.java

## Overview

`AdminControllerTest` is a critical Spring Boot integration test suite responsible for validating the security and functional integrity of the `AdminController` API. This class ensures that administrative endpoints—specifically those governing load balancing strategies—are protected by proper role-based access control (RBAC) and behave correctly under various input scenarios.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a primary gatekeeper for administrative security, modifications to this file or the associated controller should be treated as high-risk.

## Public API

The test suite validates the following API interactions:

*   **`GET /api/admin/strategy`**: Retrieves the currently active load balancing strategy.
*   **`PUT /api/admin/strategy`**: Updates the load balancing strategy. Supports both preset strategies (e.g., `LEAST_CONNECTIONS`, `ROUND_ROBIN`) and `CUSTOM` configurations with weight maps.

### Test Methods
*   `adminCanGetCurrentStrategy()`: Verifies that an authenticated `ADMIN` can successfully retrieve the current strategy.
*   `adminCanSwitchToPreset()`: Validates that an `ADMIN` can update the strategy to a predefined preset.
*   `adminCanSwitchToCustomWeights()`: Validates that an `ADMIN` can apply a custom strategy with specific resource weights.
*   `adminCanSwitchToRoundRobin()`: Validates that an `ADMIN` can switch to the `ROUND_ROBIN` preset.
*   `invalidStrategyReturnsBadRequest()`: Ensures the API rejects requests with non-existent strategy types.
*   `operatorCannotSwitchStrategy()`: Verifies that users with `OPERATOR` roles are forbidden from modifying strategies.
*   `operatorCannotGetStrategy()`: Verifies that `OPERATOR` roles are forbidden from viewing strategy configurations.
*   `unauthenticatedCannotAccess()`: Ensures that requests without valid credentials receive a `401 Unauthorized` response.

## Dependencies

*   **Spring Boot Test**: Uses `@SpringBootTest` and `@AutoConfigureMockMvc` for full context integration testing.
*   **TestContainers**: Imports `TestContainersConfig` to ensure tests run against a real infrastructure environment rather than mocks.
*   **JUnit 5**: The standard testing framework for assertions and lifecycle management.
*   **MockMvc**: Used for simulating HTTP requests and validating response statuses and JSON payloads.
*   **JwtService**: Used to generate valid `ADMIN` and `OPERATOR` tokens for security testing.
*   **JsonUtil**: Utility for serializing `StrategyRequest` DTOs into JSON strings.

## Usage Notes

### Security Testing
The test suite relies on `adminToken()` and `operatorToken()` helper methods to generate JWTs. When adding new test cases, ensure you are testing against the correct `Role` to maintain the integrity of the RBAC policy.

### Handling Hotspot Complexity
Because this file is a high-activity hotspot, developers should:
1.  **Regression Testing**: Always run this suite before pushing changes to the `AdminController` or the underlying security configuration.
2.  **Edge Cases**: When adding new strategies, ensure you add a corresponding test case for `invalidStrategyReturnsBadRequest` to prevent malformed requests from reaching the service layer.
3.  **Payload Validation**: The `adminCanSwitchToCustomWeights` test serves as a template for testing complex JSON payloads. Use `JsonUtil` to maintain consistency with production serialization.

### Example: Adding a new test case
To test a new administrative action, follow the pattern of injecting the `MockMvc` and using the `adminToken()`:

```java
@Test
void adminCanPerformNewAction() throws Exception {
    mvc.perform(post("/api/admin/new-action")
            .header("Authorization", "Bearer " + adminToken())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
}
```

Failure to maintain these tests can lead to privilege escalation vulnerabilities or broken administrative workflows.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/ScalingControllerTest.java

## Overview

`ScalingControllerTest` is a comprehensive integration test suite for the `ScalingController` API. It validates the REST endpoints responsible for managing and monitoring the cloud scaling infrastructure. 

**Note:** This file is a **HOTSPOT** within the repository, exhibiting high change frequency and complexity. It serves as a critical gatekeeper for scaling logic, and modifications to this file or the underlying `ScalingController` should be approached with caution as they directly impact system stability and resource management.

## Public API

The test suite validates the following API interactions:

*   **`GET /api/scaling/status`**: Retrieves the current scaling policy and status.
*   **`PUT /api/scaling/policy`**: Updates the active scaling policy configuration.
*   **`POST /api/scaling/trigger`**: Manually triggers a scaling action (e.g., `SCALE_UP`).

### Test Methods
*   `getScalingStatusReturnsDefaultPolicy()`: Verifies that the system initializes with a valid default policy.
*   `getScalingStatusRequiresAuthentication()`: Ensures endpoints are protected against unauthenticated access.
*   `viewerCannotAccessScalingStatus()`: Validates Role-Based Access Control (RBAC), ensuring `VIEWER` roles cannot perform administrative scaling tasks.
*   `updatePolicyPersistsAndReturns()`: Confirms that policy updates are correctly persisted to the database and reflected in subsequent status checks.
*   `triggerScaleUpAddsWorkerAndReturnsStatus()`: Tests the integration between the trigger endpoint and the worker management system.
*   `invalidPolicyReturnsBadRequest()`: Validates input sanitization and business rule enforcement (e.g., `minWorkers` cannot exceed `maxWorkers`).

## Dependencies

*   **Spring Boot Test**: Provides the testing framework and `MockMvc` for simulating HTTP requests.
*   **TestContainers**: Used to spin up ephemeral infrastructure (e.g., databases) to ensure tests run in an isolated, production-like environment.
*   **Internal Services**:
    *   `ScalingPolicyService`: Manages policy lifecycle and reloading.
    *   `JwtService`: Handles token generation for authentication testing.
    *   `ScalingPolicyRepository` / `WorkerRepository`: Used for state verification and cleanup.

## Usage Notes

### Authentication
The test suite utilizes helper methods `adminToken()` and `viewerToken()` to simulate different security contexts. When adding new tests, ensure that the appropriate `Authorization` header is included in the `MockMvc` request builder:

```java
mvc.perform(get("/api/scaling/status")
    .header("Authorization", "Bearer " + adminToken()))
    .andExpect(status().isOk());
```

### State Management
The `cleanUp()` method, annotated with `@BeforeEach`, ensures that the `ScalingPolicyRepository` is cleared and the `ScalingPolicyService` is reloaded before every test. This prevents cross-test contamination. 

### Common Pitfalls
1.  **Database State**: Because this is an integration test, failing to clean up the database or failing to reload the service state can lead to flaky tests.
2.  **Validation Logic**: The `invalidPolicyReturnsBadRequest` test highlights that the controller enforces business rules (e.g., `min > max`). Ensure that any changes to the `ScalingPolicy` model are reflected in these validation tests.
3.  **Hotspot Risk**: Given its status as a hotspot, any failure in this test suite often indicates a regression in core scaling logic rather than just a test failure. Always verify the `ScalingPolicyService` and `ScalingController` implementation when tests fail.
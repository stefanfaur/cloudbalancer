# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/api/AuthControllerTest.java

## Overview

`AuthControllerTest` is a critical integration test suite for the `dispatcher` module, responsible for validating the security contract of the `AuthController`. It ensures that the authentication lifecycle—including login, token refreshing, and logout—functions correctly under various conditions.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. Because it governs the security entry points of the application, any regressions here pose a high risk to the entire system's authorization integrity.

## Public API

The test class validates the following REST endpoints:

*   `POST /api/auth/login`: Authenticates user credentials and returns an access/refresh token pair.
*   `POST /api/auth/refresh`: Exchanges a valid refresh token for a new access/refresh token pair.
*   `POST /api/auth/logout`: Invalidates the current session and revokes the associated refresh token.

### Test Methods

| Method | Description |
| :--- | :--- |
| `loginWithValidCredentialsReturnsTokens` | Verifies successful authentication and token issuance. |
| `loginWithInvalidCredentialsReturns401` | Ensures unauthorized access is rejected with 401 status. |
| `refreshWithValidTokenReturnsNewPair` | Validates the token rotation mechanism. |
| `refreshWithInvalidTokenReturns401` | Ensures expired or malformed refresh tokens are rejected. |
| `logoutRevokesRefreshTokens` | Confirms that logging out renders the refresh token unusable. |
| `logoutWithoutAuthReturns401` | Validates that logout requires a valid Authorization header. |
| `refreshTokenRotationInvalidatesOldToken` | Ensures that once a refresh token is used, it cannot be reused (anti-replay protection). |

## Dependencies

*   **Spring Boot Test**: Uses `@SpringBootTest` and `@AutoConfigureMockMvc` to spin up the application context and simulate HTTP requests.
*   **TestContainers**: Uses `TestContainersConfig` to provide a real database environment for integration testing.
*   **UserService**: Injected to programmatically create test users before executing security flows.
*   **JsonUtil**: Used for serializing/deserializing DTOs (`LoginRequest`, `RefreshRequest`) to match the API contract.

## Usage Notes

### Integration Testing Pattern
The tests follow a stateful pattern:
1.  **Setup**: Create a user via `UserService`.
2.  **Action**: Perform a `MockMvc` request.
3.  **Assertion**: Validate HTTP status codes and JSON response structures.

### Example: Testing Token Rotation
To verify that the system correctly handles refresh token rotation (a key security feature), the test performs the following sequence:
1.  **Login**: Obtain an initial `refreshToken`.
2.  **Refresh**: Call `/api/auth/refresh` with the initial token.
3.  **Re-use Attempt**: Call `/api/auth/refresh` again with the *same* initial token.
4.  **Expectation**: The second call must return `401 Unauthorized`, proving that the system successfully invalidated the old token upon rotation.

### Pitfalls
*   **Database State**: Since this test uses `TestContainers`, ensure that the database is properly cleaned or that unique usernames are used for each test case to avoid collisions.
*   **Token Expiry**: The tests assume a specific `expiresIn` value (900 seconds). If the security configuration changes, these tests may fail if they rely on hardcoded expectations.
*   **Hotspot Risk**: Given the high change frequency, always run this suite before committing changes to `AuthController` or the underlying `SecurityConfig` classes.
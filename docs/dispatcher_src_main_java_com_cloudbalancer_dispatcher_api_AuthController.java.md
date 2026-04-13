# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AuthController.java

## Overview

The `AuthController` class is a Spring `@RestController` responsible for managing user authentication, session lifecycle, and token management within the CloudBalancer dispatcher service. It provides REST endpoints for user login, token refreshing, and secure logout operations.

The controller integrates with `UserService` for credential validation and `JwtService` for token generation, while managing persistent refresh tokens via `RefreshTokenRepository`.

## Public API

### `login`
- **Signature**: `public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request)`
- **Description**: Authenticates a user based on provided credentials. If successful, returns an `AuthResponse` containing a new access token, a refresh token, and the expiration duration. Returns `401 Unauthorized` on failure.

### `refresh`
- **Signature**: `public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request)`
- **Description**: Validates an existing refresh token. If valid and not revoked, it performs token rotation by revoking the old token and issuing a new pair of access and refresh tokens. Returns `401 Unauthorized` if the token is invalid or expired.

### `logout`
- **Signature**: `public ResponseEntity<Void> logout(Principal principal)`
- **Description**: Terminates the user session by revoking all active refresh tokens associated with the authenticated user. Returns `204 No Content` on success or `404 Not Found` if the user cannot be identified.

## Dependencies

- **Spring Framework**: `org.springframework.web.bind.annotation`, `org.springframework.http.ResponseEntity`
- **Internal Services**:
    - `UserService`: Handles authentication logic.
    - `JwtService`: Manages JWT generation and expiration settings.
    - `RefreshTokenRepository`: Handles persistence and retrieval of refresh tokens.
    - `UserRepository`: Provides access to user entities.
- **DTOs**: `AuthResponse`, `LoginRequest`, `RefreshRequest`.

## Usage Notes

- **Token Rotation**: The `refresh` endpoint implements security best practices by revoking the used refresh token immediately upon successful usage, forcing the client to use the newly issued token for subsequent requests.
- **Security Context**: The `logout` method relies on the `Principal` object provided by the Spring Security context to identify the user. Ensure that the request is authenticated before calling this endpoint.
- **Error Handling**: The controller returns standard HTTP status codes:
    - `200 OK`: Successful authentication or token refresh.
    - `204 No Content`: Successful logout.
    - `401 Unauthorized`: Failed authentication or invalid/expired refresh token.
    - `404 Not Found`: User not found during logout.
- **Internal Helper**: `createRefreshToken` is a private utility method used to generate and persist a new `RefreshToken` entity with a UUID and specific expiration time.
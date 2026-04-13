# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/JwtService.java

## Overview

The `JwtService` class is a core security component responsible for the lifecycle management of JSON Web Tokens (JWT) within the `dispatcher` module. It provides functionality to generate access tokens, extract claims (such as usernames and roles), and validate the authenticity and expiration of tokens. The service utilizes the `jjwt` library to perform cryptographic signing and parsing operations using an HMAC-SHA algorithm.

## Public API

### Constructor
*   `JwtService(String secret, long accessExpSeconds, long refreshExpSeconds)`: Initializes the service with a base64-encoded secret key and configurable expiration durations for access and refresh tokens.

### Token Generation
*   `String generateAccessToken(String username, Role role)`: Creates a signed JWT containing the provided username and role, with an expiration time based on the configured `accessTokenExpirationSeconds`.

### Token Inspection
*   `String extractUsername(String token)`: Parses the token and returns the subject (username).
*   `Role extractRole(String token)`: Parses the token and returns the user's role by mapping the "role" claim to the `Role` enum.

### Validation
*   `boolean isTokenValid(String token)`: Verifies the signature and checks if the token has expired. Returns `true` if valid, `false` otherwise.

### Configuration Accessors
*   `long getAccessTokenExpirationSeconds()`: Returns the configured access token duration.
*   `long getRefreshTokenExpirationSeconds()`: Returns the configured refresh token duration.

## Dependencies

*   **`com.cloudbalancer.common.model.Role`**: Used for type-safe role handling within JWT claims.
*   **`io.jsonwebtoken` (JJWT)**: The underlying library used for building, signing, and parsing JWTs.
*   **`org.springframework`**: Utilized for dependency injection (`@Service`) and configuration property binding (`@Value`).
*   **`javax.crypto.SecretKey`**: Represents the cryptographic key used for HMAC signing.

## Usage Notes

*   **Configuration**: The service expects the following properties to be defined in the Spring environment:
    *   `cloudbalancer.security.jwt-secret`: A base64-encoded string representing the HMAC-SHA secret key.
    *   `cloudbalancer.security.access-token-expiration-seconds`: (Optional) Defaults to 900 seconds (15 minutes).
    *   `cloudbalancer.security.refresh-token-expiration-seconds`: (Optional) Defaults to 604800 seconds (7 days).
*   **Exception Handling**: The `isTokenValid` method internally catches `JwtException` and `IllegalArgumentException`, making it safe to call during request filtering or handshake interception without crashing the application flow.
*   **Security**: Ensure the `jwt-secret` is sufficiently long and kept secure, as it is used to sign all tokens issued by the dispatcher. The service assumes the secret provided via `@Value` is already base64-encoded.
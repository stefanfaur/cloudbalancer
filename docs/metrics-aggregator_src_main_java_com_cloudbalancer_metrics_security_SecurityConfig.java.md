# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/security/SecurityConfig.java

## Overview

The `SecurityConfig` class is a core Spring `@Configuration` component that defines the security architecture for the `metrics-aggregator` service. It leverages Spring Security to enforce stateless authentication, cross-origin resource sharing (CORS) policies, and request authorization.

**Critical Note:** This file is identified as a **hotspot** within the codebase, ranking in the top 25% for both change frequency and complexity. Modifications to this file directly impact the entire service's security posture; extreme caution and thorough testing are required when altering authentication filters or CORS configurations.

## Public API

### `SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter)`
Constructor-based dependency injection for the `JwtAuthenticationFilter`. This ensures that the security chain is correctly wired with the custom JWT validation logic.

### `securityFilterChain(HttpSecurity http)`
Configures the `SecurityFilterChain` bean. It defines the security rules for the application:
*   **CORS**: Enabled via `corsConfigurationSource()`.
*   **CSRF**: Explicitly disabled, as the service uses stateless JWT authentication.
*   **Session Management**: Set to `SessionCreationPolicy.STATELESS` to ensure no HTTP sessions are created.
*   **Authorization**: All incoming requests require authentication.
*   **Exception Handling**: Returns `401 Unauthorized` for unauthenticated access attempts.
*   **Filter Ordering**: Injects `JwtAuthenticationFilter` before the standard `UsernamePasswordAuthenticationFilter`.

### `corsConfigurationSource()`
Defines the `CorsConfigurationSource` bean. It reads allowed origins from the `cloudbalancer.cors.allowed-origins` property (defaulting to `http://localhost:5173`). It permits standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`) and specific headers (`Authorization`, `Content-Type`).

## Dependencies

*   **`JwtAuthenticationFilter`**: Custom filter responsible for extracting and validating JWT tokens from incoming requests.
*   **Spring Security**: Framework used for authentication and authorization infrastructure.
*   **Jakarta Servlet API**: Used for handling HTTP response status codes during authentication failures.

## Usage Notes

### Configuration Properties
The CORS policy is configurable via the `application.properties` or `application.yml` file. To allow multiple origins, provide a comma-separated list:

```yaml
cloudbalancer:
  cors:
    allowed-origins: "https://app.cloudbalancer.com,https://admin.cloudbalancer.com"
```

### Security Implementation Rationale
*   **Statelessness**: By setting the session policy to `STATELESS`, the application is optimized for microservices architectures where authentication is handled per-request via tokens rather than server-side sessions.
*   **Filter Chain**: The `JwtAuthenticationFilter` is placed before the `UsernamePasswordAuthenticationFilter` to ensure that if a valid JWT is present, the security context is populated before the standard authentication checks occur.

### Potential Pitfalls
*   **CORS Misconfiguration**: If the `allowed-origins` property is incorrectly formatted or missing, legitimate frontend clients may receive 403 Forbidden errors during pre-flight requests.
*   **Hotspot Risk**: Because this is a high-complexity file, any changes to the `securityFilterChain` method can inadvertently open security vulnerabilities (e.g., disabling authentication for specific endpoints). Always verify changes with integration tests that simulate unauthorized access.
*   **Exception Handling**: The current implementation returns a generic `401 Unauthorized` error. If the client requires specific error codes or messages for token expiration vs. invalid signatures, the `authenticationEntryPoint` logic will need to be extended.
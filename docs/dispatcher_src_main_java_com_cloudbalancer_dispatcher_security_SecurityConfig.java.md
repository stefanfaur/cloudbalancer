# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/SecurityConfig.java

## Overview

`SecurityConfig` is the central security configuration class for the `dispatcher` service. It leverages Spring Security to enforce authentication, authorization, and cross-origin resource sharing (CORS) policies. The configuration is designed for a stateless architecture, utilizing JWT-based authentication and custom rate-limiting filters to protect the API endpoints.

## Public API

### Classes
*   **`SecurityConfig`**: The primary configuration class annotated with `@Configuration`, `@EnableWebSecurity`, and `@EnableMethodSecurity`.

### Methods
*   **`SecurityConfig(JwtAuthenticationFilter, RateLimitFilter)`**: Constructor-based dependency injection for the custom security filters.
*   **`securityFilterChain(HttpSecurity http)`**: Defines the security filter chain, including endpoint authorization rules, session management policies, and filter ordering.
*   **`passwordEncoder()`**: Provides a `BCryptPasswordEncoder` bean for secure credential hashing.
*   **`corsConfigurationSource()`**: Configures CORS settings, allowing specific origins to interact with the API.

## Dependencies

*   **`JwtAuthenticationFilter`**: Custom filter responsible for validating JWT tokens in incoming requests.
*   **`RateLimitFilter`**: Custom filter responsible for enforcing request rate limits.
*   **`application.yml`**: Provides the `cloudbalancer.cors.allowed-origins` property to dynamically configure allowed CORS origins.

## Usage Notes

### Security Architecture
*   **Statelessness**: The application is configured with `SessionCreationPolicy.STATELESS`, meaning no HTTP sessions are created or used by Spring Security. Authentication must be provided with every request via the JWT filter.
*   **Filter Chain Order**: The `JwtAuthenticationFilter` is placed before the `UsernamePasswordAuthenticationFilter`, and the `RateLimitFilter` is placed immediately after the JWT filter. This ensures that rate limiting is only applied after the user identity has been established.
*   **CORS Configuration**: CORS is enabled globally. The allowed origins are retrieved from the environment configuration (defaulting to `http://localhost:5173`). Ensure that the `cloudbalancer.cors.allowed-origins` property is correctly set in your production `application.yml` to prevent unauthorized cross-origin access.

### Endpoint Authorization
The following rules are enforced:
*   **Publicly Accessible**: `/api/auth/login`, `/api/auth/refresh`, `/api/agents/register`, `/internal/**`, `/api/tasks/*/logs/stream`, and `/api/ws/events`.
*   **Authenticated Only**: `/api/auth/logout` and all other unspecified endpoints.

### Implementation Pitfalls
*   **CSRF Protection**: CSRF is explicitly disabled (`csrf.disable()`). This is standard for stateless REST APIs using JWTs, but ensure that your client-side implementation does not rely on cookie-based authentication, which would be vulnerable to CSRF.
*   **Unauthorized Handling**: The `authenticationEntryPoint` is configured to return a `401 Unauthorized` status code directly, bypassing any default Spring Security login redirects. This is critical for maintaining a pure API-driven interface.
*   **CORS Origins**: If the `allowedOrigins` property contains a comma-separated list, the `corsConfigurationSource` splits this string to populate the `AllowedOrigins` list. Ensure there are no trailing spaces in the configuration string to avoid unexpected behavior.
# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/security/JwtAuthenticationFilter.java

## Overview

`JwtAuthenticationFilter` is a core security component within the `metrics-aggregator` module. It extends Spring Security's `OncePerRequestFilter` to intercept incoming HTTP requests, validate JSON Web Tokens (JWTs) provided in the `Authorization` header, and populate the Spring `SecurityContext` with authentication details. This ensures that downstream services can rely on a verified user identity and associated roles for every request.

## Public API

### `JwtAuthenticationFilter`
*   **Constructor**: `JwtAuthenticationFilter(JwtService jwtService)`
    *   Initializes the filter with the required `JwtService` dependency for token parsing and validation.

### `doFilterInternal`
*   **Signature**: `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`
    *   **Description**: The primary execution method for the filter. It extracts the `Bearer` token from the request header, validates it via `JwtService`, and if valid, creates a `UsernamePasswordAuthenticationToken` containing the user's identity and granted authorities (roles). This authentication object is then set in the `SecurityContextHolder`.

## Dependencies

*   **`com.cloudbalancer.common.model.Role`**: Used to map token-extracted roles to Spring Security authorities.
*   **`jakarta.servlet`**: Provides the standard Servlet API for request/response handling and filter chaining.
*   **`org.springframework.security`**: Provides the core security infrastructure, including `SecurityContextHolder` and `UsernamePasswordAuthenticationToken`.
*   **`org.springframework.web.filter.OncePerRequestFilter`**: The base class ensuring the filter is executed exactly once per request cycle.
*   **`JwtService`**: An injected service responsible for token validation, username extraction, and role retrieval.

## Usage Notes

*   **Integration**: This component is annotated with `@Component`, allowing it to be automatically detected and registered by Spring's component scanning. It is intended to be integrated into the Spring Security filter chain configuration.
*   **Token Format**: The filter expects the `Authorization` header to follow the standard `Bearer <token>` format. If the header is missing or malformed, the filter will proceed without setting an authentication object, allowing the request to be handled by subsequent security filters (which may reject the request if access is restricted).
*   **Role Mapping**: The filter automatically prefixes roles extracted from the JWT with `ROLE_` to comply with Spring Security's naming conventions for `GrantedAuthority`.
*   **Security Context**: Once the filter successfully authenticates a request, the `SecurityContext` remains populated for the duration of the request thread, allowing controllers or services to access user details via `SecurityContextHolder.getContext().getAuthentication()`.
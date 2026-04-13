# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/JwtAuthenticationFilter.java

## Overview

`JwtAuthenticationFilter` is a Spring `@Component` that extends `OncePerRequestFilter`. It serves as the primary security interceptor for the `dispatcher` service, responsible for validating JWT tokens provided in the `Authorization` header of incoming HTTP requests. By extracting user identity and roles from the token, it populates the Spring Security `SecurityContext`, enabling role-based access control for subsequent request processing.

## Public API

### Class: `JwtAuthenticationFilter`

#### Constructor
*   `JwtAuthenticationFilter(JwtService jwtService)`: Initializes the filter with the required `JwtService` dependency for token parsing and validation.

#### Methods
*   `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`: 
    *   Intercepts incoming requests.
    *   Checks for the presence of a `Bearer` token in the `Authorization` header.
    *   If valid, extracts the username and role from the token.
    *   Creates a `UsernamePasswordAuthenticationToken` and updates the `SecurityContextHolder`.
    *   Proceeds with the `FilterChain` regardless of authentication status to allow for public endpoints.

## Dependencies

*   **`com.cloudbalancer.common.model.Role`**: Used to map token roles to Spring Security authorities.
*   **`JwtService`**: An external service (injected) used to validate tokens, extract usernames, and retrieve user roles.
*   **`jakarta.servlet`**: Provides the standard Servlet API for request/response handling.
*   **`org.springframework.security`**: Provides the core authentication infrastructure, including `SecurityContextHolder` and `UsernamePasswordAuthenticationToken`.
*   **`org.springframework.web.filter.OncePerRequestFilter`**: The base class ensuring the filter is executed exactly once per request.

## Usage Notes

*   **Integration**: This filter should be registered within the `SecurityConfig` to ensure it is included in the Spring Security filter chain.
*   **Token Format**: The filter expects the `Authorization` header to follow the standard `Bearer <token>` format.
*   **Role Mapping**: The filter automatically prefixes the role extracted from the token with `ROLE_` (e.g., a role of `ADMIN` becomes `ROLE_ADMIN`) to comply with Spring Security's authority naming conventions.
*   **Security Context**: If the token is invalid or missing, the filter does not throw an exception; it simply allows the request to proceed, relying on subsequent security configuration (e.g., `HttpSecurity` rules) to deny access to protected resources.
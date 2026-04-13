# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/RateLimitFilter.java

## Overview

`RateLimitFilter` is a Spring-based `OncePerRequestFilter` component responsible for enforcing request rate limits on the dispatcher service. It utilizes the `bucket4j` library to implement a token bucket algorithm, ensuring that incoming traffic is throttled based on user identity or IP address to prevent service abuse and ensure fair resource distribution.

## Public API

### `RateLimitFilter(RateLimitProperties rateLimitProperties)`
Constructs the filter with the provided `RateLimitProperties`, which define the specific threshold configurations for different user roles and anonymous traffic.

### `doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`
The core execution method that intercepts incoming HTTP requests. 
- **Logic**: 
    - Identifies the requester via `SecurityContextHolder` (authenticated users) or `request.getRemoteAddr()` (anonymous users).
    - Retrieves or creates a `Bucket` associated with the identified key.
    - If a token is available (`bucket.tryConsume(1)`), the request proceeds.
    - If the limit is exceeded, it returns an HTTP 429 (Too Many Requests) status code with a "Retry-After" header set to 60 seconds.

### `createBucket(int tokensPerMinute)`
A private helper method that initializes a new `Bucket` instance. It configures a `Bandwidth` limit with a greedy refill strategy, replenishing the specified number of tokens every minute.

## Dependencies

- **`io.github.bucket4j`**: Provides the core token bucket implementation.
- **`jakarta.servlet`**: Standard Servlet API for request/response handling.
- **`org.springframework.security`**: Used for extracting authentication and authorization details from the security context.
- **`org.springframework.web`**: Provides the base `OncePerRequestFilter` class.
- **`RateLimitProperties`**: A custom configuration class (injected) that holds the threshold values for rate limiting.

## Usage Notes

- **Concurrency**: The filter uses a `ConcurrentHashMap` to store buckets, ensuring thread-safe access when multiple requests arrive simultaneously.
- **Identification**: 
    - Authenticated users are tracked by their username (`user:{username}`).
    - Anonymous users are tracked by their IP address (`ip:{remote_addr}`).
- **Configuration**: Ensure `RateLimitProperties` is correctly configured in the application context, as the filter relies on these values to determine the `limit` for different roles (e.g., ADMIN, USER, ANONYMOUS).
- **HTTP Response**: When a rate limit is hit, the filter terminates the chain and returns a JSON error body: `{"error":"Rate limit exceeded"}`.
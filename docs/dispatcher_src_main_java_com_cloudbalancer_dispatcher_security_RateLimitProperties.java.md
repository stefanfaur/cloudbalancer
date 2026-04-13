# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/RateLimitProperties.java

## Overview

`RateLimitProperties` is a Spring `@ConfigurationProperties` component that manages the rate-limiting thresholds for various user roles within the `cloudbalancer` system. It maps configuration values defined under the `cloudbalancer.security.rate-limit` prefix to Java fields, allowing for externalized configuration via `application.yml` or `application.properties`.

## Public API

### Class: `RateLimitProperties`

#### Methods
*   **`getAdmin()` / `setAdmin(int)`**: Accessor for the admin role rate limit (default: 200).
*   **`getOperator()` / `setOperator(int)`**: Accessor for the operator role rate limit (default: 100).
*   **`getViewer()` / `setViewer(int)`**: Accessor for the viewer role rate limit (default: 60).
*   **`getApiClient()` / `setApiClient(int)`**: Accessor for the API client role rate limit (default: 120).
*   **`getAnonymous()` / `setAnonymous(int)`**: Accessor for the anonymous/unauthenticated user rate limit (default: 10).
*   **`getLimitForRole(String role)`**: Retrieves the configured integer limit based on the provided security role string (e.g., "ROLE_ADMIN").

## Dependencies

*   `org.springframework.boot.context.properties.ConfigurationProperties`: Used to bind external configuration properties to the class fields.
*   `org.springframework.stereotype.Component`: Marks the class as a Spring-managed bean.

## Usage Notes

*   **Configuration Binding**: To override default values, define the following properties in your configuration file:
    ```yaml
    cloudbalancer:
      security:
        rate-limit:
          admin: 500
          operator: 250
          viewer: 100
          api-client: 200
          anonymous: 5
    ```
*   **Role Mapping**: The `getLimitForRole` method uses a switch expression to map standard security roles to their respective limits. If a role does not match the known constants, it defaults to the `anonymous` limit.
*   **Integration**: This class is intended to be injected into security filters (such as `RateLimitFilter`) to dynamically determine the allowed request frequency for incoming traffic.
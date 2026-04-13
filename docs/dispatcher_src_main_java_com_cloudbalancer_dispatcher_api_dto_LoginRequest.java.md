# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/LoginRequest.java

## Overview

The `LoginRequest` class is a Java `record` located in the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a Data Transfer Object (DTO) designed to encapsulate authentication credentials required for user login operations within the cloud balancer system.

## Public API

### `LoginRequest`

```java
public record LoginRequest(String username, String password) {}
```

*   **`username`**: A `String` representing the unique identifier for the user attempting to authenticate.
*   **`password`**: A `String` representing the user's secret credential.

## Dependencies

This class is a standard Java `record` and does not depend on any external libraries or internal project modules beyond the Java SE runtime.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, the `username` and `password` fields cannot be modified.
*   **Serialization**: This record is intended to be used as a payload for incoming API requests. It is compatible with standard JSON serialization libraries (such as Jackson) commonly used in Spring Boot or similar Java frameworks.
*   **Security**: Ensure that instances of `LoginRequest` are handled over secure channels (HTTPS) to prevent the exposure of the `password` field during transit. Avoid logging the contents of this object in plain text.
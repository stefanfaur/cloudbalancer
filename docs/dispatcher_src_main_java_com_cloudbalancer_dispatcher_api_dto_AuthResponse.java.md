# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AuthResponse.java

## Overview

The `AuthResponse` class is a Java `record` located in the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a Data Transfer Object (DTO) designed to encapsulate the authentication credentials returned to a client after a successful login or token refresh operation.

## Public API

### `AuthResponse` Record

```java
public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
```

#### Components
*   **`accessToken`** (`String`): The primary JSON Web Token (JWT) used for authorizing subsequent requests to protected resources.
*   **`refreshToken`** (`String`): A token used to obtain a new `accessToken` once the current one expires, without requiring the user to re-authenticate.
*   **`expiresIn`** (`long`): The duration, typically in seconds, representing the time remaining before the `accessToken` becomes invalid.

## Dependencies

This class is a standard Java `record` and does not rely on external libraries or internal project dependencies beyond the Java SE runtime environment.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified.
*   **Serialization**: This DTO is intended to be serialized into JSON format when sent as a response body from the Dispatcher service. Ensure that your JSON serialization framework (e.g., Jackson) is configured to support Java records.
*   **Integration**: This class is typically used by the authentication controller or service layer to return structured data to the client-side application or API consumer.

**Maintainer**: sfaur
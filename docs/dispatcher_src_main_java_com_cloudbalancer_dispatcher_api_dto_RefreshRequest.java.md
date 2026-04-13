# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/RefreshRequest.java

## Overview

The `RefreshRequest` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a lightweight container for transmitting authentication refresh tokens between the client and the dispatcher service.

## Public API

### `RefreshRequest`

```java
public record RefreshRequest(String refreshToken) {}
```

#### Constructors
*   **`RefreshRequest(String refreshToken)`**: Initializes a new instance with the provided refresh token string.

#### Methods
*   **`refreshToken()`**: Returns the `String` value of the refresh token stored in this record.

## Dependencies

This class has no external dependencies and relies solely on standard Java 16+ `record` functionality.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once a `RefreshRequest` is instantiated, the `refreshToken` cannot be modified.
*   **Serialization**: This DTO is intended to be serialized/deserialized (e.g., via Jackson) when receiving JSON payloads from API consumers. Ensure that the JSON field name matches the record component name (`refreshToken`) or configure your JSON mapper accordingly.
*   **Primary Maintainer**: sfaur
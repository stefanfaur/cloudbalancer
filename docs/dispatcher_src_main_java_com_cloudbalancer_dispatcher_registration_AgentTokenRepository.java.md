# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/registration/AgentTokenRepository.java

## Overview

The `AgentTokenRepository` is a Spring Data JPA repository interface located in the `com.cloudbalancer.dispatcher.registration` package. It serves as the data access layer for `AgentToken` entities, providing standard CRUD operations and custom query capabilities to manage agent authentication tokens within the cloud balancer system.

## Public API

### `AgentTokenRepository` (Interface)
Extends `JpaRepository<AgentToken, UUID>`, inheriting standard persistence methods such as `save`, `findById`, `delete`, and `findAll`.

### `findByTokenHash(String tokenHash)`
Retrieves an `AgentToken` entity by its hashed token value.

*   **Parameters**: `tokenHash` (String) - The hashed representation of the agent token.
*   **Returns**: `Optional<AgentToken>` - An `Optional` containing the found entity, or `Optional.empty()` if no matching token hash exists.

## Dependencies

- `org.springframework.data.jpa.repository.JpaRepository`: Provides the base JPA repository functionality.
- `java.util.Optional`: Used for safe handling of nullable return values.
- `java.util.UUID`: Used as the primary key type for the `AgentToken` entity.

## Usage Notes

- **Integration**: This repository is primarily utilized by `AgentTokenService` to validate agent authentication requests. The `validate` method in the service layer relies on `findByTokenHash` to verify the existence and validity of tokens provided by agents.
- **Data Access**: As a Spring Data JPA repository, the implementation is generated at runtime by the Spring Framework. Ensure that the `AgentToken` entity is correctly annotated with `@Entity` and mapped to the underlying database schema.
- **Security**: The `tokenHash` parameter implies that tokens are stored in a hashed format rather than plain text. Ensure that the hashing algorithm used during token generation matches the one expected by the database query.
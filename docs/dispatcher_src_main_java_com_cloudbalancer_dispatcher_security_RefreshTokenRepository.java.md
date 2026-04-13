# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/RefreshTokenRepository.java

## Overview

The `RefreshTokenRepository` is a Spring Data JPA repository interface responsible for managing the persistence of `RefreshToken` entities. It provides an abstraction layer for database operations related to OAuth2 or JWT-based refresh tokens, enabling the system to track, validate, and revoke user sessions.

## Public API

### `RefreshTokenRepository` (Interface)
Extends `JpaRepository<RefreshToken, Long>`, inheriting standard CRUD operations for the `RefreshToken` entity.

### `findByToken`
```java
Optional<RefreshToken> findByToken(String token)
```
Retrieves a `RefreshToken` entity by its unique string representation. Returns an `Optional` containing the token if found, or `Optional.empty()` otherwise.

### `findByUserAndRevokedFalse`
```java
List<RefreshToken> findByUserAndRevokedFalse(User user)
```
Retrieves a list of all active (non-revoked) refresh tokens associated with a specific `User`. This is primarily used during logout processes or when enforcing session limits.

## Dependencies

- `org.springframework.data.jpa.repository.JpaRepository`: Provides the base framework for JPA-based data access.
- `java.util.List`: Used for returning collections of active tokens.
- `java.util.Optional`: Used for safe handling of nullable query results.

## Usage Notes

- **Session Management**: This repository is a critical component of the authentication flow. The `findByUserAndRevokedFalse` method is specifically invoked by the `AuthController` during logout operations to invalidate existing sessions.
- **Security**: Ensure that the `token` field in the underlying database is indexed to maintain performance, as `findByToken` is a frequent lookup operation during authentication refresh requests.
- **Integration**: This repository is tested via `RefreshTokenRepositoryTest`, which verifies the persistence logic and query accuracy against an embedded or test database.
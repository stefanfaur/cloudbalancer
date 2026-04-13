# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/registration/AgentTokenService.java

## Overview

The `AgentTokenService` is a core security service within the `dispatcher` module responsible for managing the lifecycle of agent authentication tokens. It handles the generation, validation, and revocation of tokens used by cloud agents to authenticate with the system.

The service employs a secure hashing mechanism (SHA-256) to store tokens, ensuring that plaintext tokens are never persisted in the database. Tokens are generated with a specific prefix (`cb_at_`) and sufficient entropy to prevent brute-force attacks.

## Public API

### `AgentTokenService(AgentTokenRepository repository)`
Constructs the service with the required `AgentTokenRepository` for persistence operations.

### `CreateTokenResult create(String label, String createdBy)`
Generates a new secure agent token.
- **Parameters**: `label` (a human-readable identifier), `createdBy` (the user or system initiating the request).
- **Returns**: A `CreateTokenResult` record containing the unique ID, the plaintext token (returned only once), and the label.

### `boolean validate(String plaintext)`
Validates a provided plaintext token against stored hashes.
- **Parameters**: `plaintext` (the token provided by the agent).
- **Returns**: `true` if the token is valid and not revoked; `false` otherwise. Updates the `lastUsedAt` timestamp upon successful validation.

### `void revoke(UUID id)`
Revokes an existing agent token by its unique identifier.
- **Parameters**: `id` (the UUID of the token to revoke).

### `List<AgentToken> listAll()`
Retrieves a list of all registered agent tokens.
- **Returns**: A list of `AgentToken` entities.

### `static String sha256(String input)`
A utility method that computes the SHA-256 hash of a given string. Used internally for token verification and storage.

## Dependencies

- **Spring Framework**: Utilizes `@Service` for component scanning and `@Transactional` for database integrity.
- **Java Security**: Uses `SecureRandom` for cryptographically strong token generation and `MessageDigest` for hashing.
- **Persistence**: Relies on `AgentTokenRepository` for CRUD operations on `AgentToken` entities.

## Usage Notes

- **Security Warning**: The plaintext token is only returned by the `create` method. It cannot be retrieved again from the system once the method returns. Ensure the calling service or user securely stores the returned plaintext token.
- **Transactional Integrity**: Methods that modify the state of tokens (`create`, `validate`, `revoke`) are marked as `@Transactional` to ensure consistency within the database.
- **Token Format**: All generated tokens follow the format `cb_at_<hex-encoded-random-bytes>`.
- **Validation Side-Effects**: The `validate` method updates the `lastUsedAt` field in the database. Frequent validation calls will result in frequent database writes.
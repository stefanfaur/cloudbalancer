# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/UserService.java

## Overview

The `UserService` class is a core security component within the `dispatcher` module. It provides essential identity management services, including user authentication, account creation, and system initialization checks. It leverages Spring Security's `PasswordEncoder` to ensure that user credentials are stored and verified securely.

## Public API

### `UserService(UserRepository userRepository, PasswordEncoder passwordEncoder)`
Constructor for dependency injection. Initializes the service with the required data access layer and password hashing utility.

### `Optional<User> authenticate(String username, String password)`
Authenticates a user by verifying their credentials against the stored records.
*   **Parameters**: `username` (String), `password` (String)
*   **Returns**: An `Optional` containing the `User` if authentication is successful and the account is enabled; otherwise, an empty `Optional`.

### `User createUser(String username, String rawPassword, Role role)`
Creates and persists a new user in the system.
*   **Parameters**: `username` (String), `rawPassword` (String), `role` (Role)
*   **Returns**: The persisted `User` object with an encoded password.

### `boolean hasUsers()`
Checks if any users exist in the system. This is typically used during application bootstrap to determine if an initial administrative account needs to be created.
*   **Returns**: `true` if at least one user exists, `false` otherwise.

## Dependencies

*   **`com.cloudbalancer.common.model.Role`**: Defines the authorization levels for users.
*   **`org.springframework.security.crypto.password.PasswordEncoder`**: Used for secure password hashing and verification.
*   **`org.springframework.stereotype.Service`**: Marks this class as a Spring-managed service component.
*   **`UserRepository`**: (Internal) Data access repository for `User` entities.

## Usage Notes

*   **Security**: Always use the `authenticate` method for login flows. It automatically handles the `PasswordEncoder.matches()` logic, ensuring that raw passwords are never compared directly against stored hashes.
*   **Initialization**: The `hasUsers()` method is intended to be used in conjunction with application startup logic to prevent unauthorized access or to trigger first-time setup wizards.
*   **Account Status**: The `authenticate` method enforces an `isEnabled` check. Ensure that user accounts are properly toggled in the database if access needs to be revoked.
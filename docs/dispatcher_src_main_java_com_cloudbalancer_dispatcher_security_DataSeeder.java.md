# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/security/DataSeeder.java

## Overview

`DataSeeder` is a Spring `@Component` that implements the `ApplicationRunner` interface. Its primary purpose is to ensure the system is initialized with a default administrative user if no users exist in the database upon application startup. This mechanism acts as a bootstrap utility to prevent the system from being inaccessible during initial deployment.

## Public API

### `DataSeeder` (Class)
The main entry point for the data seeding process. It is managed by the Spring container and executes automatically during the application startup lifecycle.

### `DataSeeder(UserService userService)` (Constructor)
Initializes the seeder with the required `UserService` dependency to perform user existence checks and user creation.

### `run(ApplicationArguments args)` (Method)
The implementation of the `ApplicationRunner` interface. 
- **Logic**: Checks if the system contains any users via `userService.hasUsers()`.
- **Action**: If no users are found, it creates a default administrator account with the username `admin` and password `admin` assigned the `Role.ADMIN` role.
- **Logging**: Emits a warning log entry to alert administrators that the default credentials should be changed immediately in production environments.

## Dependencies

- `com.cloudbalancer.common.model.Role`: Used to assign the appropriate security role to the seeded user.
- `org.springframework.boot.ApplicationRunner`: Interface used to trigger the seeding logic after the Spring context is initialized.
- `org.springframework.stereotype.Component`: Marks the class for component scanning.
- `org.slf4j.Logger` / `LoggerFactory`: Used for logging initialization events and security warnings.
- `UserService`: An external service dependency used to interact with the user persistence layer.

## Usage Notes

- **Security Warning**: The `DataSeeder` creates a default account with well-known credentials (`admin/admin`). It is critical to ensure that this component is either disabled or that the default credentials are changed immediately upon deployment in any environment accessible to the public.
- **Startup Lifecycle**: Because this implements `ApplicationRunner`, it runs after the Spring application context has fully loaded.
- **Idempotency**: The `run` method is idempotent; it performs a check (`hasUsers()`) before attempting to create the default user, ensuring that it does not overwrite or duplicate users if the database is already populated.
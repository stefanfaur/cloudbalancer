# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/ScalingPolicyService.java

## Overview

`ScalingPolicyService` is a core service component within the `dispatcher` module responsible for managing the system's auto-scaling configuration. It acts as an abstraction layer between the application logic and the persistent storage, ensuring that scaling parameters—such as worker bounds, cooldown periods, and scaling steps—are consistently available across the dispatcher.

The service maintains an in-memory `volatile` copy of the current policy to ensure high-performance access for the `AutoScalerService`, while synchronizing updates with the underlying database via `ScalingPolicyRepository`.

## Public API

### `ScalingPolicyService(ScalingPolicyRepository repository)`
Constructor that initializes the service with the required repository and triggers an initial load of the scaling policy from the database.

### `ScalingPolicy getCurrentPolicy()`
Returns the currently active `ScalingPolicy` held in memory. This is the primary method used by the `AutoScalerService` to make scaling decisions.

### `void reloadPolicy()`
Forces a refresh of the in-memory policy by re-reading the configuration from the database.

### `ScalingPolicy updatePolicy(ScalingPolicy policy)`
Updates the persistent scaling configuration in the database and updates the in-memory cache. If no record currently exists, a new record is created.

## Dependencies

*   **`com.cloudbalancer.common.model.ScalingPolicy`**: The domain model representing the scaling configuration.
*   **`com.cloudbalancer.dispatcher.persistence.ScalingPolicyRecord`**: The database entity representation of the scaling policy.
*   **`com.cloudbalancer.dispatcher.persistence.ScalingPolicyRepository`**: Spring Data repository for database access.
*   **`org.slf4j.Logger` / `LoggerFactory`**: Used for logging policy updates and load errors.
*   **`org.springframework.stereotype.Service`**: Marks this class as a Spring-managed service.
*   **`java.time.Duration`**: Used for handling time-based policy parameters like cooldowns and drain times.

## Usage Notes

*   **Caching Strategy**: The service uses a `volatile` field for the `currentPolicy`. This ensures that changes made via `updatePolicy` or `reloadPolicy` are immediately visible to all threads, providing thread-safe access without explicit locking.
*   **Default Fallback**: If the database is empty or an error occurs during retrieval, the service automatically falls back to `ScalingPolicy.defaults()` to ensure the system remains operational.
*   **Persistence**: The `updatePolicy` method performs an "upsert" logic: it attempts to find an existing record to update; if none is found, it creates a new one.
*   **Integration**: This service is heavily utilized by `AutoScalerService` for real-time decision-making and by `ScalingController` for exposing configuration management via the API.
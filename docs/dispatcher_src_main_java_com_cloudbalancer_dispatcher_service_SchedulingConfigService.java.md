# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/SchedulingConfigService.java

## Overview

The `SchedulingConfigService` is a core service within the `dispatcher` module responsible for managing and persisting the active load-balancing strategy. It acts as the bridge between the persistent storage (`SchedulingConfigRepository`) and the runtime execution of scheduling logic. 

The service ensures that the system maintains a consistent state of the scheduling strategy across restarts by loading the configuration from the database upon initialization, defaulting to `ROUND_ROBIN` if no configuration is found or if an error occurs.

## Public API

### `SchedulingConfigService(SchedulingConfigRepository configRepository)`
Constructor that initializes the service with the required repository and triggers the initial loading of the scheduling configuration.

### `SchedulingStrategy getCurrentStrategy()`
Returns the currently active `SchedulingStrategy` instance. This method is thread-safe as the underlying strategy reference is marked `volatile`.

### `void switchStrategy(String name, Map<String, Integer> weights)`
Updates the active scheduling strategy. This method performs the following actions:
1. Persists the new strategy name and weight configuration to the database.
2. Updates the internal `currentStrategy` reference using the `SchedulingStrategyFactory`.
3. Logs the change for audit purposes.

## Dependencies

- `com.cloudbalancer.dispatcher.persistence.SchedulingConfigRecord`: Data model for storing configuration.
- `com.cloudbalancer.dispatcher.persistence.SchedulingConfigRepository`: Interface for database operations.
- `com.cloudbalancer.dispatcher.scheduling.SchedulingStrategy`: Interface for strategy implementations.
- `com.cloudbalancer.dispatcher.scheduling.SchedulingStrategyFactory`: Factory class used to instantiate strategies based on name and weights.
- `org.springframework.stereotype.Service`: Spring framework annotation for component scanning.

## Usage Notes

- **Initialization**: The service attempts to load the configuration from the database immediately upon bean instantiation. If the database is unreachable or empty, it defaults to `ROUND_ROBIN` with empty weights.
- **Thread Safety**: The `currentStrategy` field is declared `volatile`, ensuring that updates made via `switchStrategy` are immediately visible to all threads calling `getCurrentStrategy`.
- **Persistence**: The `switchStrategy` method assumes a singleton configuration record in the database. It will either update the existing record or create a new one if none exists.
- **Error Handling**: The `loadOrDefault` method is wrapped in a try-catch block to prevent application startup failure in the event of database connectivity issues, defaulting to a safe fallback strategy instead.
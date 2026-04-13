# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/SchedulingConfigRepository.java

## Overview

The `SchedulingConfigRepository` is a Spring Data JPA repository interface responsible for the persistence layer operations of `SchedulingConfigRecord` entities. It provides standard CRUD (Create, Read, Update, Delete) functionality and query capabilities for scheduling configurations within the `dispatcher` module.

By extending `JpaRepository`, this interface leverages Spring Data's automated implementation generation, allowing the application to interact with the underlying database without requiring boilerplate SQL or manual DAO implementation.

## Public API

The `SchedulingConfigRepository` interface inherits all methods from `JpaRepository<SchedulingConfigRecord, Long>`, including:

*   `save(S entity)`: Persists a given `SchedulingConfigRecord`.
*   `findById(Long id)`: Retrieves a configuration record by its primary key.
*   `findAll()`: Returns all stored scheduling configuration records.
*   `deleteById(Long id)`: Removes a configuration record by its primary key.
*   `existsById(Long id)`: Checks for the existence of a record with the given ID.

## Dependencies

*   **Spring Data JPA**: `org.springframework.data.jpa.repository.JpaRepository`
*   **Domain Model**: `com.cloudbalancer.dispatcher.persistence.SchedulingConfigRecord` (Implicitly required as the entity type for the repository).

## Usage Notes

*   **Integration**: This repository is primarily injected into the `SchedulingConfigService` to facilitate the persistence of load-balancing strategies.
*   **Transaction Management**: As a standard Spring Data repository, operations are typically wrapped in transactional proxies when called from service-layer components marked with `@Transactional`.
*   **Testing**: Persistence logic is validated via the `SchedulingConfigRepositoryTest` suite, which ensures that database interactions and entity mapping function correctly within the Spring context.
*   **Primary Key**: The repository expects a `Long` type for the primary key, corresponding to the `@Id` field defined in the `SchedulingConfigRecord` entity.
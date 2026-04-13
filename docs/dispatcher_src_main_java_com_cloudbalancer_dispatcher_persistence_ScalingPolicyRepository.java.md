# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/ScalingPolicyRepository.java

## Overview

`ScalingPolicyRepository` is a persistence interface located in the `dispatcher` module. It serves as the Data Access Object (DAO) layer for managing `ScalingPolicyRecord` entities within the system. By extending Spring Data JPA's `JpaRepository`, it provides standard CRUD operations and query capabilities for auto-scaling configurations stored in the underlying relational database.

## Public API

The `ScalingPolicyRepository` interface inherits the following methods from `JpaRepository<ScalingPolicyRecord, Long>`:

*   **`save(S entity)`**: Persists a given `ScalingPolicyRecord`.
*   **`findById(ID id)`**: Retrieves a `ScalingPolicyRecord` by its unique identifier.
*   **`findAll()`**: Returns all stored scaling policy records.
*   **`deleteById(ID id)`**: Removes a scaling policy record from the database.
*   **`existsById(ID id)`**: Checks if a policy record exists for the given ID.
*   **`count()`**: Returns the total number of scaling policy records.

## Dependencies

*   **`org.springframework.data.jpa.repository.JpaRepository`**: The core Spring Data JPA interface that provides the repository abstraction.
*   **`com.cloudbalancer.dispatcher.persistence.ScalingPolicyRecord`**: The domain entity managed by this repository.

## Usage Notes

*   **Integration**: This repository is primarily consumed by the `ScalingPolicyService` to perform database operations related to auto-scaling configurations.
*   **Transaction Management**: As a Spring Data JPA repository, operations are typically wrapped in transactional proxies managed by the service layer. Ensure that calls to this repository are made within an appropriate `@Transactional` context if data consistency is required.
*   **Extensibility**: Custom query methods (e.g., finding policies by specific criteria) can be added to this interface using Spring Data's query derivation or `@Query` annotations if the standard CRUD methods are insufficient for future requirements.
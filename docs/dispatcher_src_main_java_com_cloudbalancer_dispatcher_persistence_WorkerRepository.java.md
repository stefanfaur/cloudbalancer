# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/WorkerRepository.java

## Overview

The `WorkerRepository` is a Spring Data JPA repository interface that provides data access operations for `WorkerRecord` entities within the `dispatcher` module. It facilitates the retrieval of worker information based on their health status and associated agent identifiers, serving as the primary persistence interface for worker management.

## Public API

### `WorkerRepository`
The interface extends `JpaRepository<WorkerRecord, String>`, inheriting standard CRUD operations for `WorkerRecord` entities.

### Methods

*   **`findByHealthState(WorkerHealthState state)`**
    *   Retrieves a list of all workers matching the specified `WorkerHealthState`.
*   **`findByAgentId(String agentId)`**
    *   Retrieves a list of all workers associated with a specific agent ID.
*   **`findByAgentIdAndHealthStateIn(String agentId, Collection<WorkerHealthState> states)`**
    *   Retrieves a list of workers associated with a specific agent ID that fall within a provided collection of health states.

## Dependencies

*   `com.cloudbalancer.common.model.WorkerHealthState`: Used to filter workers by their operational status.
*   `org.springframework.data.jpa.repository.JpaRepository`: Provides the base framework for JPA-based data access.
*   `java.util.List`: Used for returning collections of `WorkerRecord` entities.

## Usage Notes

*   **Integration**: This repository is utilized by the `WorkerRegistryService` to manage worker states and by the `AgentController` to expose worker data via the API.
*   **Query Derivation**: The methods rely on Spring Data JPA's query derivation mechanism. Ensure that the field names in `WorkerRecord` (e.g., `healthState`, `agentId`) match the method signatures exactly to avoid runtime errors.
*   **Performance**: When using `findByAgentIdAndHealthStateIn`, ensure the `states` collection is not null or empty to prevent unexpected query results or performance degradation.
*   **Testing**: Persistence logic for this repository is validated via the `WorkerRecordRepositoryTest` suite.
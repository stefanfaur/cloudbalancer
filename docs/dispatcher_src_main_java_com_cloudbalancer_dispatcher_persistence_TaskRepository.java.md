# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/TaskRepository.java

## Overview

The `TaskRepository` interface is a Spring Data JPA repository responsible for managing the persistence of `TaskRecord` entities. It provides a robust abstraction layer for database operations, enabling the application to query, filter, and aggregate task data based on their lifecycle states and worker assignments. By extending `JpaRepository` and `JpaSpecificationExecutor`, it supports both standard CRUD operations and complex dynamic querying.

## Public API

### Methods

*   **`findByState(TaskState state)`**: Retrieves a list of all tasks currently in the specified `TaskState`.
*   **`findByAssignedWorkerId(String workerId)`**: Retrieves a list of all tasks assigned to a specific worker identified by their ID.
*   **`findByStateIn(Collection<TaskState> states)`**: Retrieves a list of tasks that match any of the provided `TaskState` values.
*   **`findByAssignedWorkerIdAndStateIn(String workerId, Collection<TaskState> states)`**: Retrieves a list of tasks assigned to a specific worker that are also in one of the provided `TaskState` values.
*   **`countByState(TaskState state)`**: Returns the total count of tasks currently in the specified `TaskState`.

## Dependencies

*   **`com.cloudbalancer.common.model.TaskState`**: Defines the enumeration of possible task lifecycle states.
*   **`org.springframework.data.jpa.repository.JpaRepository`**: Provides standard JPA-based repository functionality.
*   **`org.springframework.data.jpa.repository.JpaSpecificationExecutor`**: Enables the use of JPA Criteria API for dynamic query generation.
*   **`java.util.Collection` / `java.util.List`**: Used for handling collections of task records and states.
*   **`java.util.UUID`**: Used as the primary key type for `TaskRecord` entities.

## Usage Notes

*   **Integration**: This repository is primarily utilized by service-layer components such as `AutoScalerService` and `DashboardWebSocketHandler` to monitor system load and task distribution.
*   **Performance**: The `countByState` method is frequently invoked by the `AutoScalerService` to determine if the task queue is empty, which is critical for scaling decisions.
*   **Dynamic Queries**: Because the interface extends `JpaSpecificationExecutor`, developers can implement complex filtering logic beyond the predefined methods by passing `Specification` objects to the repository.
*   **Transactional Integrity**: As a standard Spring Data JPA repository, all methods are executed within the context of the calling service's transaction. Ensure that callers are annotated with `@Transactional` where read-consistency or write-atomicity is required.
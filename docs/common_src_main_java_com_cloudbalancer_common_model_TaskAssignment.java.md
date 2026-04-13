# File: common/src/main/java/com/cloudbalancer/common/model/TaskAssignment.java

## Overview

The `TaskAssignment` class is an immutable data carrier (Java `record`) used within the `com.cloudbalancer.common.model` package. It represents the binding between a specific computational task and the worker node assigned to execute it. This model is central to the task scheduling and tracking lifecycle within the CloudBalancer system.

## Public API

### `TaskAssignment` (Record)

The `TaskAssignment` record encapsulates the following fields:

*   **`UUID taskId`**: The unique identifier of the task being assigned.
*   **`TaskDescriptor descriptor`**: The metadata and configuration details defining the task's requirements and behavior.
*   **`String assignedWorkerId`**: The unique identifier of the worker node selected to perform the task.
*   **`Instant assignedAt`**: The timestamp indicating when the assignment was created.
*   **`UUID executionId`**: A unique identifier for the specific execution instance of this task.

## Dependencies

*   `java.time.Instant`: Used for precise timestamping of the assignment.
*   `java.util.UUID`: Used for generating and storing unique identifiers for tasks and executions.
*   `com.cloudbalancer.common.model.TaskDescriptor`: A dependency representing the task's definition (referenced in the constructor).

## Usage Notes

*   **Immutability**: As a Java `record`, instances of `TaskAssignment` are immutable. Once an assignment is created, its fields cannot be modified.
*   **Data Integrity**: This class is intended to be used as a Data Transfer Object (DTO) when communicating between the scheduler and the worker nodes.
*   **Serialization**: Being a standard Java record, it is compatible with most JSON serialization libraries (e.g., Jackson, Gson) typically used in the CloudBalancer ecosystem for inter-service communication.
*   **Primary Maintainer**: sfaur
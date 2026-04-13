# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/WorkerSummary.java

## Overview

The `WorkerSummary` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a lightweight container for representing the current status and metadata of a worker node within the cloud balancer system.

## Public API

### Constructor
`public WorkerSummary(String id, String healthState, String agentId, int activeTaskCount, String registeredAt)`

Creates a new instance of `WorkerSummary` with the following fields:

*   **`id`** (`String`): The unique identifier for the worker instance.
*   **`healthState`** (`String`): The current operational status of the worker (e.g., "HEALTHY", "UNHEALTHY", "MAINTENANCE").
*   **`agentId`** (`String`): The identifier of the agent process managing the worker.
*   **`activeTaskCount`** (`int`): The number of tasks currently being processed by this worker.
*   **`registeredAt`** (`String`): The timestamp indicating when the worker was registered with the dispatcher.

### Accessor Methods
As a Java record, the class automatically provides accessor methods matching the field names:
*   `id()`
*   `healthState()`
*   `agentId()`
*   `activeTaskCount()`
*   `registeredAt()`

## Dependencies

This class is a standard Java `record` and does not depend on any external libraries or internal project classes outside of the standard Java Development Kit (JDK).

## Usage Notes

*   **Immutability**: Being a `record`, instances of `WorkerSummary` are immutable. Once created, the state of the worker summary cannot be modified.
*   **Serialization**: This DTO is intended to be serialized (typically to JSON) when transmitted between the dispatcher and client-facing APIs or monitoring dashboards.
*   **Data Integrity**: The `registeredAt` field is stored as a `String`. Ensure that the value provided conforms to the expected ISO-8601 date-time format used across the system for consistent parsing.
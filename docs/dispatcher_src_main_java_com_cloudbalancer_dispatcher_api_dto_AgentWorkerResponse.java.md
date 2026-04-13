# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AgentWorkerResponse.java

## Overview

The `AgentWorkerResponse` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a structured representation of the current status and metadata of a worker agent managed by the cloud balancer system.

## Public API

### Constructors
*   `AgentWorkerResponse(String workerId, String healthState, int activeTaskCount, String registeredAt)`: Constructs a new `AgentWorkerResponse` instance with the specified worker details.

### Accessors
*   `workerId()`: Returns the unique identifier of the worker.
*   `healthState()`: Returns the current operational status of the worker (e.g., "HEALTHY", "UNHEALTHY").
*   `activeTaskCount()`: Returns the number of tasks currently being processed by the worker.
*   `registeredAt()`: Returns the timestamp string indicating when the worker was registered.

## Dependencies

This class is a standard Java `record` and does not have any external library dependencies. It relies solely on the Java SE platform.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified.
*   **Serialization**: This DTO is intended for use in API responses. It is compatible with standard JSON serialization libraries (such as Jackson or Gson) commonly used in Spring Boot or other Java web frameworks.
*   **Data Integrity**: Ensure that the `registeredAt` string follows a consistent ISO-8601 format if it is intended to be parsed by client-side applications.
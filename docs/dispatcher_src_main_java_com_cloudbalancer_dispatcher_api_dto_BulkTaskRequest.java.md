# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/BulkTaskRequest.java

## Overview

The `BulkTaskRequest` class is a Data Transfer Object (DTO) used within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a container for batch processing operations, specifically designed to encapsulate a collection of task identifiers that require dispatching or status updates.

This class is implemented as a Java `record`, providing an immutable and concise way to transport data across the system layers.

## Public API

### Constructors

*   **`BulkTaskRequest(List<UUID> taskIds)`**
    *   Creates a new instance of `BulkTaskRequest` containing the specified list of task identifiers.

### Methods

*   **`List<UUID> taskIds()`**
    *   Returns the list of `UUID` objects representing the tasks included in the bulk request.

## Dependencies

*   `java.util.List`: Used to store the collection of task identifiers.
*   `java.util.UUID`: Used to uniquely identify individual tasks within the request.

## Usage Notes

*   **Immutability**: As a Java `record`, the `BulkTaskRequest` is immutable. Once instantiated, the list of `taskIds` cannot be modified.
*   **Validation**: This DTO does not perform internal validation. It is recommended that service-layer components validate that the `taskIds` list is neither null nor empty before processing the request.
*   **Serialization**: Being a standard Java record, this class is compatible with common JSON serialization libraries (such as Jackson or Gson) used in RESTful API controllers for the `cloudbalancer` project.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/BulkReprioritizeRequest.java

## Overview

The `BulkReprioritizeRequest` is a Java record used within the `dispatcher` module to encapsulate a request for updating the priority levels of multiple tasks simultaneously. It serves as a Data Transfer Object (DTO) to facilitate batch processing of priority adjustments across the cloud balancing system.

## Public API

### `BulkReprioritizeRequest`

A record representing the batch update command.

*   **Constructor**: `BulkReprioritizeRequest(List<UUID> taskIds, Priority priority)`
*   **Fields**:
    *   `List<UUID> taskIds`: A list of unique identifiers for the tasks that require a priority update.
    *   `Priority priority`: The new `Priority` level to be assigned to the specified tasks.

## Dependencies

*   `com.cloudbalancer.common.model.Priority`: Defines the enumeration of available priority levels.
*   `java.util.List`: Used for the collection of task identifiers.
*   `java.util.UUID`: Used for the unique identification of individual tasks.

## Usage Notes

*   **Immutability**: As a Java record, this class is immutable. Once instantiated, the list of task IDs and the target priority cannot be modified.
*   **Batch Processing**: This DTO is intended for use in API endpoints where multiple tasks need to be re-queued or re-prioritized in a single operation to reduce network overhead and improve performance.
*   **Validation**: Ensure that the `taskIds` list is not null or empty before processing, and verify that the `priority` object is a valid member of the `Priority` enum.
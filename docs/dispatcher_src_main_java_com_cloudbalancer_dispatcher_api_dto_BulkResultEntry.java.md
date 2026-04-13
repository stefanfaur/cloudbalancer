# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/BulkResultEntry.java

## Overview

The `BulkResultEntry` class is a Java `record` used to encapsulate the outcome of an individual task execution within a bulk operation. It provides a lightweight, immutable data carrier for reporting the status of specific tasks processed by the cloud balancer dispatcher.

## Public API

### `BulkResultEntry`
A record representing the result of a single task.

**Constructor:**
`public BulkResultEntry(UUID taskId, boolean success, String reason)`

**Components:**
*   `UUID taskId()`: The unique identifier of the task that was processed.
*   `boolean success()`: Indicates whether the task execution was successful (`true`) or failed (`false`).
*   `String reason()`: A descriptive message explaining the outcome, typically used to provide error details if `success` is `false`.

## Dependencies

*   `java.util.UUID`: Used for the unique identification of tasks.

## Usage Notes

*   **Immutability**: As a Java `record`, all fields are final and immutable. Once a `BulkResultEntry` is instantiated, its state cannot be modified.
*   **Bulk Processing**: This class is intended to be used in collections (e.g., `List<BulkResultEntry>`) when returning the results of batch operations to the caller.
*   **Error Handling**: When `success` is set to `false`, the `reason` field should be populated with a meaningful error message or exception summary to assist in debugging or retry logic.
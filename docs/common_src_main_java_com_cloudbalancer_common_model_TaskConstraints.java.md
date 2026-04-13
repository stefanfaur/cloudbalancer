# File: common/src/main/java/com/cloudbalancer/common/model/TaskConstraints.java

## Overview

The `TaskConstraints` class is an immutable Java `record` located in the `com.cloudbalancer.common.model` package. It defines the operational boundaries and requirements for task execution within the CloudBalancer system. By encapsulating tag requirements and worker-specific restrictions, it provides a standardized way to filter or validate worker eligibility for specific tasks.

## Public API

### `TaskConstraints` (Record)
- **`requiredTags()`**: Returns a `Set<String>` representing the tags a worker must possess to be eligible for the task.
- **`blacklistedWorkers()`**: Returns a `Set<String>` of worker identifiers that are explicitly excluded from performing the task.
- **`whitelistedWorkers()`**: Returns a `Set<String>` of worker identifiers that are explicitly permitted to perform the task.

### `unconstrained()` (Static Method)
- **Signature**: `public static TaskConstraints unconstrained()`
- **Description**: A factory method that returns a default `TaskConstraints` instance where all sets are empty. This represents a task with no specific requirements or restrictions.

## Dependencies

- `java.util.Set`: Used for storing collections of tags and worker identifiers.

## Usage Notes

- **Immutability**: As a Java `record`, `TaskConstraints` is immutable. Any modification to constraints requires the creation of a new instance.
- **Default State**: Use the `unconstrained()` method when initializing tasks that do not require specific worker filtering or tag matching.
- **Set Semantics**: The implementation relies on `Set.of()`, meaning the collections returned are unmodifiable. Attempting to add or remove elements from these sets will result in an `UnsupportedOperationException`.
- **Primary Maintainer**: sfaur
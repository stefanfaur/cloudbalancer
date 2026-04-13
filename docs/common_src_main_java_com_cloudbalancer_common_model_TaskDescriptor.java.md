# File: common/src/main/java/com/cloudbalancer/common/model/TaskDescriptor.java

## Overview

`TaskDescriptor.java` defines the core data model for task definitions within the CloudBalancer system. It is implemented as a Java `record`, providing an immutable structure for representing task configurations, including execution specifications, resource requirements, and scheduling policies.

The class is annotated with `@JsonInclude(JsonInclude.Include.NON_NULL)`, ensuring that serialized JSON output excludes null fields, thereby reducing payload size and improving interoperability with external services.

## Public API

### `withDefaults()`

Returns a new instance of `TaskDescriptor` where null-valued fields are populated with system-defined defaults.

*   **Behavior**:
    *   `constraints`: Defaults to `TaskConstraints.unconstrained()` if null.
    *   `priority`: Defaults to `Priority.NORMAL` if null.
    *   `executionPolicy`: Defaults to `ExecutionPolicy.defaults()` if null.
    *   `resourceProfile` and `io`: Remain unchanged (null values are preserved as these are considered legitimately optional).

## Dependencies

*   `com.fasterxml.jackson.annotation.JsonInclude`: Used for JSON serialization configuration.
*   `java.util.Map`: Used for the flexible `executionSpec` field.
*   `com.cloudbalancer.common.model.TaskConstraints`: Referenced for default constraint values.
*   `com.cloudbalancer.common.model.Priority`: Referenced for default priority levels.
*   `com.cloudbalancer.common.model.ExecutionPolicy`: Referenced for default execution policies.

## Usage Notes

*   **Immutability**: As a Java `record`, `TaskDescriptor` is immutable. The `withDefaults()` method does not modify the existing object but returns a new instance with the necessary fields populated.
*   **Integration**: This class is frequently utilized by the `dispatcher` service. Specifically, it is invoked during the creation of `TaskRecord` objects to ensure that incoming task requests are normalized with standard system defaults before persistence.
*   **Optionality**: Developers should be aware that while `constraints`, `priority`, and `executionPolicy` are normalized by `withDefaults()`, `resourceProfile` and `io` are designed to remain optional and should be handled with null-checks in downstream logic.
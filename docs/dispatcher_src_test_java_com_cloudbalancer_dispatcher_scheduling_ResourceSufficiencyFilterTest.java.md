# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/ResourceSufficiencyFilterTest.java

## Overview

`ResourceSufficiencyFilterTest` is a JUnit 5 test suite designed to validate the logic within the `ResourceSufficiencyFilter` class. The filter is responsible for determining which workers in a cluster have sufficient remaining capacity (CPU, memory, and disk) to accommodate a specific incoming task.

## Public API

The test class does not expose a public API as it is a test suite. It exercises the following methods of the `ResourceSufficiencyFilter`:

- `filter(Task task, List<Worker> workers)`: Evaluates a list of workers against a task's resource requirements and returns a filtered list containing only those capable of hosting the task.

## Dependencies

- **JUnit 5 (`org.junit.jupiter.api.Test`)**: Used for defining and executing test cases.
- **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertions on the filtering results.
- **`FilterTestHelper`**: A utility class providing helper methods (`taskWithResources`, `workerWithCapacity`) to instantiate domain objects for testing scenarios.

## Usage Notes

- The tests verify resource sufficiency based on the remaining capacity of a worker (Total Capacity - Allocated Resources).
- **Scenarios covered**:
    - **Over-allocation**: Ensures workers with insufficient free CPU are excluded.
    - **Exact Match**: Validates that workers with exactly enough resources to satisfy a task are included.
    - **Resource Specificity**: Confirms that if any single resource dimension (e.g., memory) is insufficient, the worker is excluded, even if other resources (e.g., CPU, disk) are abundant.
- These tests rely on the `FilterTestHelper` to abstract the creation of `Task` and `Worker` objects, ensuring test readability and maintainability.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/ConstraintFilterTest.java

## Overview

`ConstraintFilterTest` is a JUnit 5 test suite that validates the functionality of the `ConstraintFilter` class. This component is responsible for filtering a list of available worker nodes based on specific task requirements, including required tags, blacklists, and whitelists. The tests ensure that the scheduling logic correctly identifies eligible workers by evaluating individual and combined constraints.

## Public API

The test class does not expose a public API as it is a test suite. However, it validates the following behaviors of the `ConstraintFilter` class:

*   **`enforcesRequiredTags`**: Verifies that only workers possessing all mandatory tags defined in the task are returned.
*   **`enforcesBlacklist`**: Verifies that workers explicitly excluded in the task's blacklist are removed from the result set.
*   **`enforcesWhitelist`**: Verifies that only workers explicitly included in the task's whitelist are returned.
*   **`unconstrainedKeepsAll`**: Verifies that if a task has no constraints, all provided workers are returned.
*   **`combinedConstraints`**: Verifies that the filter correctly applies multiple constraints simultaneously (e.g., requiring a specific tag AND being present on a whitelist).

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api.Test`)**: Used for defining and executing test cases.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertions on the filtered worker lists.
*   **`FilterTestHelper`**: A utility class providing factory methods to create mock tasks and worker records for testing scenarios.
*   **`ConstraintFilter`**: The target class under test.

## Usage Notes

*   These tests rely on `FilterTestHelper` to generate mock data. Ensure that any changes to the `Task` or `Worker` data structures are reflected in the helper methods to prevent compilation errors in the test suite.
*   The tests assume that the `ConstraintFilter.filter()` method returns a `List` of workers that satisfy all provided criteria.
*   To run these tests, execute the standard Maven or Gradle test lifecycle command:
    ```bash
    mvn test -Dtest=ConstraintFilterTest
    ```
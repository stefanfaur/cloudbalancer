# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/HealthFilterTest.java

## Overview

`HealthFilterTest` is a unit test suite for the `HealthFilter` class, which is responsible for filtering worker nodes based on their current `WorkerHealthState`. The test ensures that the dispatcher correctly identifies and excludes workers that are not in a state suitable for receiving new tasks.

## Public API

The `HealthFilterTest` class contains the following test methods:

*   **`removesUnhealthyWorkers()`**: Verifies that workers in `SUSPECT` or `DEAD` states are filtered out, leaving only `HEALTHY` workers.
*   **`keepsAllHealthyWorkers()`**: Ensures that multiple workers in the `HEALTHY` state are correctly retained.
*   **`returnsEmptyWhenNoneHealthy()`**: Confirms that the filter returns an empty list when no workers meet the health criteria.
*   **`stoppingWorkersAreExcludedFromScheduling()`**: Validates that workers in the `STOPPING` state are excluded from the scheduling pool.
*   **`excludesDrainingWorkers()`**: Ensures that workers in the `DRAINING` state are excluded, while allowing `RECOVERING` and `HEALTHY` workers to pass through.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api.Test`)**: Used for defining and executing test cases.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertions on the filtered results.
*   **`com.cloudbalancer.common.model.WorkerHealthState`**: Defines the health states used to categorize workers.
*   **`FilterTestHelper`**: A utility class providing helper methods (e.g., `workerRecord`, `anyTask`) to simplify the creation of test data.

## Usage Notes

*   This test suite assumes the existence of a `HealthFilter` class that implements a filtering logic based on the `WorkerHealthState` enum.
*   The tests rely on `FilterTestHelper` to generate mock worker records. Ensure that the helper methods are correctly configured to match the expected data structure of the `WorkerRecord` objects used in production.
*   These tests are intended to be run as part of the standard Maven/Gradle build lifecycle for the `dispatcher` module.
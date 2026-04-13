# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/scheduling/ResourceAvailabilityScorerTest.java

## Overview

`ResourceAvailabilityScorerTest` is a JUnit 5 test suite designed to validate the logic within the `ResourceAvailabilityScorer` class. The scorer is responsible for calculating a numerical score (0-100) representing the available capacity of a worker node, which is a critical component in the task scheduling pipeline of the CloudBalancer system.

## Public API

The class contains the following test methods:

*   `fullyFreeWorkerScoresHigh()`: Verifies that a worker with zero allocated resources receives a maximum score of 100.
*   `fullyAllocatedWorkerScoresZero()`: Verifies that a worker with zero remaining capacity receives a score of 0.
*   `halfAllocatedWorkerScoresAround50()`: Ensures that a worker with 50% resource utilization receives a score within the 45-55 range.
*   `scoreIsWithinRange()`: Validates that the scoring algorithm consistently returns values within the normalized 0-100 range regardless of input.

## Dependencies

*   **JUnit 5 (`org.junit.jupiter.api.Test`)**: Used for defining and executing test cases.
*   **AssertJ (`org.assertj.core.api.Assertions`)**: Used for fluent assertion syntax.
*   **`FilterTestHelper`**: A utility class providing helper methods (e.g., `workerWithCapacity`, `anyTask`) to generate mock worker and task objects for testing scenarios.
*   **`ResourceAvailabilityScorer`**: The system-under-test (SUT) located in the `com.cloudbalancer.dispatcher.scheduling` package.

## Usage Notes

*   **Test Data**: The tests utilize `FilterTestHelper` to simulate various worker states (CPU cores, RAM, and disk capacity). Ensure that any changes to the `Worker` model or `ResourceAvailabilityScorer` logic are reflected in the helper methods to prevent test failures.
*   **Scoring Logic**: The scorer assumes a linear normalization of resources. If the scoring algorithm is updated to account for non-linear resource weighting (e.g., prioritizing CPU over RAM), the `halfAllocatedWorkerScoresAround50` test may require adjustment.
*   **Execution**: These tests are intended to run as part of the standard Maven/Gradle build lifecycle. They do not require external infrastructure or network access.
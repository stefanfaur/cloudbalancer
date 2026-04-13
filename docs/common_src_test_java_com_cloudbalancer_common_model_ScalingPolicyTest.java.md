# File: common/src/test/java/com/cloudbalancer/common/model/ScalingPolicyTest.java

## Overview

`ScalingPolicyTest` is a JUnit 5 test suite designed to verify the integrity and validation logic of the `ScalingPolicy` model. It ensures that the policy correctly enforces business rules regarding worker counts, cooldown periods, and scaling step increments.

## Public API

### `ScalingPolicyTest`

| Method | Description |
| :--- | :--- |
| `defaultsAreCorrect()` | Verifies that the `ScalingPolicy.defaults()` factory method returns the expected baseline configuration. |
| `validateRejectsMinGreaterThanMax()` | Ensures that an `IllegalArgumentException` is thrown when `minWorkers` exceeds `maxWorkers`. |
| `validateRejectsNegativeCooldown()` | Ensures that an `IllegalArgumentException` is thrown when a negative `cooldownPeriod` is provided. |
| `validateRejectsStepOutOfRange()` | Ensures that an `IllegalArgumentException` is thrown when `scaleUpStep` falls outside the permitted range (1-3). |

## Dependencies

- **JUnit 5 (Jupiter)**: Used for test lifecycle management and assertions.
- **AssertJ**: Used for fluent assertions (`assertThat`, `assertThatThrownBy`).
- **java.time.Duration**: Used for defining and validating time-based policy parameters.
- **`com.cloudbalancer.common.model.ScalingPolicy`**: The target class under test.

## Usage Notes

- This test suite serves as a specification for the validation constraints of the `ScalingPolicy` object.
- When modifying the `ScalingPolicy` constructor or its `validated` factory method, ensure these tests are updated to reflect changes in business rules (e.g., if the allowed range for `scaleUpStep` changes).
- The tests utilize `assertThatThrownBy` to verify that the domain model correctly guards against invalid state transitions or configurations.
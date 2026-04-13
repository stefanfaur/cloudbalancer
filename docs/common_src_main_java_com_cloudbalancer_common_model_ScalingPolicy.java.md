# File: common/src/main/java/com/cloudbalancer/common/model/ScalingPolicy.java

## Overview

The `ScalingPolicy` record defines the configuration parameters for the cluster auto-scaling engine within the `cloudbalancer` system. It encapsulates the constraints and thresholds required to manage worker node lifecycle, including minimum/maximum capacity, cooldown intervals, and scaling step increments.

This class serves as a data carrier and a validation gateway, ensuring that any scaling configuration applied to the system adheres to operational safety standards.

## Public API

### `defaults()`
Returns a pre-configured `ScalingPolicy` instance with standard production defaults:
*   **minWorkers**: 2
*   **maxWorkers**: 20
*   **cooldownPeriod**: 3 minutes
*   **scaleUpStep**: 1
*   **scaleDownStep**: 1
*   **scaleDownDrainTime**: 60 seconds

### `validated(int minWorkers, int maxWorkers, Duration cooldownPeriod, int scaleUpStep, int scaleDownStep, Duration scaleDownDrainTime)`
Constructs a new `ScalingPolicy` instance after verifying that all parameters fall within acceptable operational ranges.

**Throws:**
*   `IllegalArgumentException`: If `minWorkers` > `maxWorkers`.
*   `IllegalArgumentException`: If `cooldownPeriod` or `scaleDownDrainTime` are zero or negative.
*   `IllegalArgumentException`: If `scaleUpStep` is not between 1 and 3 (inclusive).
*   `IllegalArgumentException`: If `scaleDownStep` is less than 1.

## Dependencies

*   `java.time.Duration`: Used for defining time-based intervals such as cooldowns and drain periods.

## Usage Notes

*   **Immutability**: As a Java `record`, `ScalingPolicy` is immutable. Once created, its values cannot be modified. To change a policy, create a new instance using the `validated` method.
*   **Validation**: Always prefer using the `validated` factory method over the default record constructor when accepting user or configuration input to ensure system stability.
*   **Integration**: This model is used by the `ScalingController` to enforce cluster constraints during runtime updates and is verified by `ScalingPolicyTest` to ensure business logic integrity.
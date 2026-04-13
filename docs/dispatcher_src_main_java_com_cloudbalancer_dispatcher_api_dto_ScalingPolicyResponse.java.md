# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/ScalingPolicyResponse.java

## Overview

The `ScalingPolicyResponse` is a Java `record` used to encapsulate the state of a scaling policy at a specific point in time. It serves as a Data Transfer Object (DTO) within the `dispatcher` module, facilitating the communication of policy configurations alongside their last modification timestamp.

## Public API

### `ScalingPolicyResponse`

A immutable record containing the following components:

*   **`policy`**: An instance of `com.cloudbalancer.common.model.ScalingPolicy` representing the configuration details of the scaling policy.
*   **`updatedAt`**: An `java.time.Instant` representing the precise timestamp when the policy was last updated or retrieved.

#### Constructor
```java
public ScalingPolicyResponse(ScalingPolicy policy, Instant updatedAt)
```

## Dependencies

*   `com.cloudbalancer.common.model.ScalingPolicy`: The core domain model representing the scaling logic and thresholds.
*   `java.time.Instant`: Standard Java library for representing a specific point on the timeline.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the policy and timestamp cannot be modified.
*   **Serialization**: This DTO is intended for use in API responses. Ensure that your JSON serialization framework (e.g., Jackson) is configured to handle `java.time.Instant` correctly (typically requiring the `jackson-datatype-jsr310` module).
*   **Maintainer**: Primary maintenance is handled by **sfaur**.
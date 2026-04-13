# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/ScalingPolicyRequest.java

## Overview

The `ScalingPolicyRequest` is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It encapsulates the configuration parameters required to define or update a scaling policy for cloud resources managed by the dispatcher.

## Public API

### `ScalingPolicyRequest`

A immutable data carrier representing the scaling configuration.

| Field | Type | Description |
| :--- | :--- | :--- |
| `minWorkers` | `int` | The minimum number of worker instances to maintain. |
| `maxWorkers` | `int` | The maximum number of worker instances allowed. |
| `cooldownSeconds` | `int` | The duration in seconds to wait between scaling actions. |
| `scaleUpStep` | `int` | The number of instances to add during a scale-up event. |
| `scaleDownStep` | `int` | The number of instances to remove during a scale-down event. |
| `drainTimeSeconds` | `int` | The grace period in seconds to allow tasks to complete before terminating an instance. |

## Dependencies

This class is a standard Java `record` and has no external dependencies beyond the Java Development Kit (JDK) 16+.

## Usage Notes

*   **Immutability**: As a `record`, all fields are final and immutable. Once an instance is created, its values cannot be modified.
*   **Validation**: This class does not perform internal validation. It is recommended to use Jakarta Bean Validation annotations (e.g., `@Min`, `@Max`) or manual validation logic in the service layer when receiving this object from external API requests to ensure values like `minWorkers` are not negative and `minWorkers <= maxWorkers`.
*   **Serialization**: Being a standard Java record, it is fully compatible with JSON serialization libraries such as Jackson or Gson, typically used in REST controllers to map incoming HTTP request bodies.
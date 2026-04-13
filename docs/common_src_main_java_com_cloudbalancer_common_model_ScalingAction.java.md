# File: common/src/main/java/com/cloudbalancer/common/model/ScalingAction.java

## Overview

The `ScalingAction` enum defines the set of permissible operations that the CloudBalancer auto-scaling engine can perform on a cluster. It serves as a centralized source of truth for scaling logic, ensuring consistency across the infrastructure when determining how to adjust resource capacity.

## Public API

The `ScalingAction` enum provides the following constants:

| Constant | Description |
| :--- | :--- |
| `SCALE_UP` | Indicates that the system should increase the number of resources or instances. |
| `SCALE_DOWN` | Indicates that the system should decrease the number of resources or instances. |
| `NONE` | Indicates that no scaling action is required or should be performed. |

## Dependencies

This enum is a standalone component within the `com.cloudbalancer.common.model` package and does not depend on any external libraries or other internal classes.

## Usage Notes

*   **Type Safety**: Use this enum to enforce strict typing when passing scaling instructions between the dispatcher and the scaling engine.
*   **Integration**: This enum is mirrored in the frontend via `web-dashboard/src/api/types.ts` as a union type to maintain parity between the backend API and the user interface.
*   **Extensibility**: When adding new scaling behaviors (e.g., `SUSPEND` or `MAINTENANCE`), ensure that both the backend enum and the corresponding frontend type definitions are updated to prevent serialization mismatches.
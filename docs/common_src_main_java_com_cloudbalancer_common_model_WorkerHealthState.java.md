# File: common/src/main/java/com/cloudbalancer/common/model/WorkerHealthState.java

## Overview

`WorkerHealthState` is an enumeration that defines the operational lifecycle and health status of worker nodes within the CloudBalancer system. It acts as the canonical source of truth for node state management, ensuring consistency across the dispatcher, backend services, and the web dashboard.

## Public API

The `WorkerHealthState` enum provides the following constants:

*   **`HEALTHY`**: The worker is fully operational and capable of processing tasks.
*   **`SUSPECT`**: The worker is exhibiting signs of instability or latency, triggering further diagnostic checks.
*   **`DEAD`**: The worker is unresponsive and has been marked as offline.
*   **`RECOVERING`**: The worker is in the process of restarting or re-synchronizing its state after a failure.
*   **`DRAINING`**: The worker is finishing existing tasks and will not accept new assignments; typically used during maintenance or scale-down operations.
*   **`STOPPING`**: The worker is in the process of a controlled shutdown.

## Dependencies

This file has no external dependencies and does not import any other classes. It is a standalone component within the `com.cloudbalancer.common.model` package.

## Usage Notes

*   **Cross-Platform Consistency**: This enum is mirrored in the frontend via `web-dashboard/src/api/types.ts`. When adding new states, ensure both the Java backend and the TypeScript frontend definitions are updated to maintain synchronization.
*   **UI Integration**: The `HealthBadge` component in the web dashboard relies on these enum values to render color-coded status indicators. Changes to the enum names or the addition of new states will require corresponding updates to the `HealthBadge` mapping logic.
*   **State Transitions**: While this enum defines the possible states, the logic governing transitions between these states (e.g., `HEALTHY` -> `SUSPECT` -> `DEAD`) is handled by the dispatcher and monitoring services.
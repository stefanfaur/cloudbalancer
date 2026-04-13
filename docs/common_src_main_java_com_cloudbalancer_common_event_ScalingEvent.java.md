# File: common/src/main/java/com/cloudbalancer/common/event/ScalingEvent.java

## Overview

The `ScalingEvent` class is an immutable Java `record` that encapsulates the details of a scaling operation within the CloudBalancer system. It serves as a standardized data carrier for logging and propagating information whenever the system adjusts the number of active worker nodes.

This event tracks the state transition of the worker pool, including the specific actions taken, the trigger mechanism, and the identities of the affected worker nodes.

## Public API

### `ScalingEvent` (Record Components)

*   **`eventId`** (`String`): A unique identifier for the scaling event.
*   **`timestamp`** (`Instant`): The precise time at which the scaling decision was made.
*   **`action`** (`ScalingAction`): The type of scaling action performed (e.g., SCALE_UP, SCALE_DOWN).
*   **`triggerType`** (`ScalingTriggerType`): The source or logic that initiated the scaling event.
*   **`reason`** (`String`): A descriptive string explaining why the scaling event occurred.
*   **`previousWorkerCount`** (`int`): The number of workers active before the scaling action.
*   **`newWorkerCount`** (`int`): The number of workers active after the scaling action.
*   **`workersAdded`** (`List<String>`): A list of IDs for workers that were added to the cluster.
*   **`workersRemoved`** (`List<String>`): A list of IDs for workers that were removed from the cluster.

### Methods

*   **`eventType()`**: Returns the constant string `"SCALING_DECISION"`. This method satisfies the `CloudBalancerEvent` interface contract, allowing the event to be identified within the event processing pipeline.

## Dependencies

*   `com.cloudbalancer.common.model.ScalingAction`: Defines the nature of the scaling operation.
*   `com.cloudbalancer.common.model.ScalingTriggerType`: Defines the origin of the scaling request.
*   `java.time.Instant`: Used for precise event timestamping.
*   `java.util.List`: Used to store collections of worker identifiers.

## Usage Notes

*   **Immutability**: As a Java `record`, `ScalingEvent` is immutable. Once instantiated, its state cannot be modified, ensuring thread safety when passing events across different components of the CloudBalancer system.
*   **Event Identification**: When processing events in a generic event bus or listener, use the `eventType()` method to filter for scaling-related operations.
*   **Data Integrity**: Ensure that `previousWorkerCount` and `newWorkerCount` accurately reflect the state of the system at the time of the event to maintain auditability of the scaling history.
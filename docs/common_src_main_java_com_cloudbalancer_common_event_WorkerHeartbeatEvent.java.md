# File: common/src/main/java/com/cloudbalancer/common/event/WorkerHeartbeatEvent.java

## Overview

The `WorkerHeartbeatEvent` is an immutable Java `record` used within the CloudBalancer system to communicate the periodic health status of worker nodes. It acts as a telemetry signal, allowing the system to monitor the availability and operational integrity of individual workers across the infrastructure.

## Public API

### `eventType()`
Returns the constant string identifier for this event type.

*   **Signature**: `public String eventType()`
*   **Returns**: `"WORKER_HEARTBEAT"`

### Record Components
*   **`eventId`** (`String`): A unique identifier for the specific heartbeat instance.
*   **`timestamp`** (`Instant`): The exact time the heartbeat was generated.
*   **`workerId`** (`String`): The unique identifier of the worker node sending the heartbeat.
*   **`healthState`** (`WorkerHealthState`): The current health status of the worker (e.g., healthy, degraded, critical).

## Dependencies

*   `com.cloudbalancer.common.model.WorkerHealthState`: Used to represent the health status of the worker node.
*   `java.time.Instant`: Used for precise event timestamping.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure type consistency across the event bus.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its state cannot be modified.
*   **Event Handling**: This event is intended to be consumed by the system's monitoring or orchestration components to update the global view of worker availability.
*   **Integration**: Ensure that the `workerId` provided matches the identifier used during the initial registration process (see `WorkerRegisteredEvent`).
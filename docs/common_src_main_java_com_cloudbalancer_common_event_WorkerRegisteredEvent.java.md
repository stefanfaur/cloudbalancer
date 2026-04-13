# File: common/src/main/java/com/cloudbalancer/common/event/WorkerRegisteredEvent.java

## Overview

The `WorkerRegisteredEvent` is a Java `record` used within the CloudBalancer system to encapsulate the registration of a new worker node. It serves as a formal notification that a worker has joined the cluster, carrying essential metadata including the worker's unique identifier and its hardware or software capabilities.

This event is part of the system's event-driven architecture, facilitating communication between the worker nodes and the central control plane.

## Public API

### `WorkerRegisteredEvent` (Constructor)
Creates a new instance of the event.
*   **Parameters**:
    *   `String eventId`: A unique identifier for the specific event instance.
    *   `Instant timestamp`: The precise time the registration occurred.
    *   `String workerId`: The unique identifier of the worker node being registered.
    *   `WorkerCapabilities capabilities`: An object detailing the specifications and capabilities of the worker.

### `eventType()`
Returns the constant string identifier for this event type.
*   **Returns**: `"WORKER_REGISTERED"`

## Dependencies

*   `com.cloudbalancer.common.model.WorkerCapabilities`: Used to define the operational profile of the registered worker.
*   `java.time.Instant`: Used for precise event timestamping.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure type consistency across the event bus.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an event is instantiated, its state cannot be modified, ensuring thread safety and consistency when passing events through asynchronous message queues or event buses.
*   **Event Identification**: The `eventType()` method should be used by event consumers (e.g., listeners or dispatchers) to filter and route these events appropriately within the system.
*   **Integration**: This event is typically emitted by the worker node during its startup sequence and consumed by the cluster manager to update the active worker registry.
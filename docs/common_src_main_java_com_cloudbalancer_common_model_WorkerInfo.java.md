# File: common/src/main/java/com/cloudbalancer/common/model/WorkerInfo.java

## Overview

The `WorkerInfo` class is a Java `record` located in the `com.cloudbalancer.common.model` package. It serves as a data carrier representing the state, capabilities, and operational metrics of a worker node within the CloudBalancer infrastructure. As an immutable data structure, it encapsulates the identity and current status of a worker at a specific point in time.

## Public API

### `WorkerInfo`
A record representing the comprehensive state of a worker.

**Components:**
*   `String id()`: The unique identifier for the worker.
*   `WorkerHealthState healthState()`: The current operational health status of the worker.
*   `WorkerCapabilities capabilities()`: An object defining the hardware or software capabilities of the worker.
*   `WorkerMetrics currentMetrics()`: An object containing real-time performance and load metrics.
*   `Instant registeredAt()`: The timestamp indicating when the worker was registered with the system.

## Dependencies

*   `java.time.Instant`: Used for tracking the registration timestamp of the worker.
*   `com.cloudbalancer.common.model.WorkerHealthState` (Implicit): Represents the health status enum/class.
*   `com.cloudbalancer.common.model.WorkerCapabilities` (Implicit): Represents the capabilities model.
*   `com.cloudbalancer.common.model.WorkerMetrics` (Implicit): Represents the metrics model.

## Usage Notes

*   **Immutability**: Being a Java `record`, `WorkerInfo` is immutable. Any updates to a worker's state should result in the creation of a new `WorkerInfo` instance rather than modification of an existing one.
*   **Serialization**: This record is intended for use in data transfer scenarios (e.g., JSON serialization for REST APIs or message queues). Ensure that the associated types (`WorkerHealthState`, `WorkerCapabilities`, `WorkerMetrics`) are also serializable.
*   **Primary Maintainer**: sfaur
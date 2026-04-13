# File: common/src/main/java/com/cloudbalancer/common/event/WorkerMetricsEvent.java

## Overview

The `WorkerMetricsEvent` is a Java `record` used within the CloudBalancer system to encapsulate performance and health telemetry data reported by worker nodes. It acts as a standardized event payload for transmitting `WorkerMetrics` across the system, allowing the control plane to monitor the state of individual workers in real-time.

## Public API

### `WorkerMetricsEvent` (Record)

*   **`eventId`** (`String`): A unique identifier for the specific event instance.
*   **`timestamp`** (`Instant`): The exact time the metrics were captured or the event was generated.
*   **`workerId`** (`String`): The unique identifier of the worker node reporting the metrics.
*   **`metrics`** (`WorkerMetrics`): The payload containing the actual performance data (e.g., CPU, memory, load).

### Methods

*   **`eventType()`**: Returns the constant string `"WORKER_METRICS"`. This method is required by the `CloudBalancerEvent` interface to facilitate event routing and deserialization.

## Dependencies

*   `com.cloudbalancer.common.model.WorkerMetrics`: Used to define the structure of the performance data carried by the event.
*   `java.time.Instant`: Used for precise temporal tracking of the event.
*   `com.cloudbalancer.common.event.CloudBalancerEvent`: The interface implemented by this record to ensure type safety within the event bus.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once an instance is created, its fields cannot be modified, ensuring thread safety when passing events through asynchronous message queues or event buses.
*   **Event Routing**: The `eventType()` method should be used by event consumers to filter incoming messages. Systems listening for worker telemetry should subscribe specifically to the `"WORKER_METRICS"` type.
*   **Integration**: This event is typically produced by worker nodes during their periodic heartbeat or metrics reporting cycle and consumed by the CloudBalancer orchestrator to inform scaling decisions.
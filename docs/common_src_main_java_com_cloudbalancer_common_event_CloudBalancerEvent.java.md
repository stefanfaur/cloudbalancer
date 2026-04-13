# File: common/src/main/java/com/cloudbalancer/common/event/CloudBalancerEvent.java

## Overview

`CloudBalancerEvent` is the base sealed interface for all event-driven communications within the CloudBalancer system. It defines the mandatory metadata required for event tracking, auditing, and processing across the distributed architecture.

The interface is annotated with Jackson polymorphic type handling, allowing for seamless serialization and deserialization of various event types based on the `eventType` property.

## Public API

### Methods

*   **`String eventId()`**: Returns the unique identifier for the specific event instance.
*   **`Instant timestamp()`**: Returns the `java.time.Instant` representing when the event was generated.
*   **`String eventType()`**: Returns the string representation of the event type, used for polymorphic dispatching.

## Dependencies

*   `java.time.Instant`: Used for standardized event timestamping.
*   `com.fasterxml.jackson.annotation.JsonTypeInfo`: Used to enable polymorphic JSON serialization.
*   `com.fasterxml.jackson.annotation.JsonSubTypes`: Used to map specific event implementations to their corresponding JSON type names.

## Usage Notes

*   **Sealed Hierarchy**: This interface is `sealed`, meaning only the explicitly permitted classes can implement it. This ensures type safety and exhaustive pattern matching when handling events.
*   **Polymorphic Serialization**: When serializing or deserializing events, ensure the JSON payload includes the `eventType` field, as it is the discriminator property defined in the `@JsonTypeInfo` annotation.
*   **Permitted Implementations**:
    *   `TaskSubmittedEvent`
    *   `TaskStateChangedEvent`
    *   `TaskCompletedEvent`
    *   `WorkerRegisteredEvent`
    *   `WorkerHeartbeatEvent`
    *   `WorkerMetricsEvent`
    *   `TaskAssignedEvent`
    *   `TaskDeadLetteredEvent`
    *   `ScalingEvent`
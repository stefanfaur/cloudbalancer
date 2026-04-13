# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/WorkerRecord.java

## Overview

`WorkerRecord` is a JPA-annotated entity representing the persistent state of a worker node within the CloudBalancer system. It acts as the primary source of truth for worker metadata, resource allocation, and lifecycle status (e.g., draining, recovering, or stopping).

**Warning**: This file is a **HOTSPOT** within the codebase, exhibiting high change frequency and complexity. Modifications to this class directly impact the persistence layer and state machine logic. Exercise extreme caution when altering resource tracking logic or database mapping, as these changes can lead to inconsistent cluster state or data corruption.

## Public API

### Resource Management
*   `allocateResources(ResourceProfile profile)`: Increments the worker's allocated CPU, memory, and disk counters, and increments the active task count.
*   `releaseResources(ResourceProfile profile)`: Decrements the worker's resource counters and active task count, ensuring values do not drop below zero.
*   `resetLedger()`: Resets all resource allocation counters and the active task count to zero.

### State Accessors
*   **Identity**: `getId()`, `getAgentId()`, `setAgentId(String)`
*   **Health**: `getHealthState()`, `setHealthState(WorkerHealthState)`
*   **Capabilities**: `getCapabilities()`, `setCapabilities(WorkerCapabilities)`
*   **Lifecycle Timestamps**:
    *   `getRegisteredAt()`, `setRegisteredAt(Instant)`
    *   `getRecoveryStartedAt()`, `setRecoveryStartedAt(Instant)`
    *   `getDrainStartedAt()`, `setDrainStartedAt(Instant)`
    *   `getStoppingStartedAt()`, `setStoppingStartedAt(Instant)`
*   **Metrics**: `getAllocatedCpu()`, `getAllocatedMemoryMb()`, `getAllocatedDiskMb()`, `getActiveTaskCount()`

## Dependencies

*   `jakarta.persistence.*`: Used for ORM mapping to the underlying database.
*   `com.cloudbalancer.common.model.ResourceProfile`: Used for resource delta calculations.
*   `com.cloudbalancer.common.model.WorkerCapabilities`: Encapsulates worker hardware/software constraints.
*   `com.cloudbalancer.common.model.WorkerHealthState`: Defines the current operational status of the worker.
*   `java.time.Instant`: Used for precise lifecycle event tracking.

## Usage Notes

### Resource Tracking
The `allocateResources` and `releaseResources` methods perform manual arithmetic on the entity's fields. Because these methods are not thread-safe, ensure that any calls to these methods are performed within a transactional context (e.g., `@Transactional` service methods) to prevent race conditions during concurrent task scheduling.

### Lifecycle Management
The `WorkerRecord` tracks specific timestamps for state transitions:
1.  **Drain**: Use `setDrainStartedAt` when a worker is marked for decommissioning to allow the `WorkerRegistryService` to gracefully migrate tasks.
2.  **Recovery**: Use `setRecoveryStartedAt` when the system detects a worker in a degraded state that requires re-synchronization.
3.  **Stopping**: Use `setStoppingStartedAt` when the `HeartbeatTracker` initiates a kill signal for an unresponsive node.

### Implementation Pitfalls
*   **JSON Conversion**: The `capabilities` field uses a custom `WorkerCapabilitiesConverter`. Ensure that any changes to the `WorkerCapabilities` structure are reflected in the converter to avoid deserialization errors.
*   **Database Constraints**: The entity enforces `nullable = false` on core fields like `healthState` and `registeredAt`. Ensure these are populated during the instantiation of new `WorkerRecord` objects via the public constructor.
*   **Hotspot Risk**: Given its status as a high-complexity hotspot, any logic changes to resource arithmetic should be accompanied by comprehensive unit tests covering edge cases (e.g., releasing resources on an already empty ledger).
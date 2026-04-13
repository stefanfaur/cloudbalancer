# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/service/ResourceLedgerInitializer.java

## Overview

`ResourceLedgerInitializer` is a Spring `@Component` responsible for synchronizing the state of the resource ledger upon application startup. It ensures that the system's internal tracking of available worker resources is consistent with the current environment state immediately after the Spring application context has fully initialized.

## Public API

### `ResourceLedgerInitializer(WorkerRegistryService workerRegistryService)`
Constructs a new `ResourceLedgerInitializer` with the required `WorkerRegistryService`.

*   **Parameters**: 
    *   `workerRegistryService` - The service responsible for managing worker registrations and ledger persistence.

### `onStartup()`
An event listener method triggered by the `ApplicationReadyEvent`. It logs the initialization process and invokes the `rebuildResourceLedger` method on the `WorkerRegistryService`.

## Dependencies

*   `org.slf4j.Logger` / `org.slf4j.LoggerFactory`: Used for logging initialization progress.
*   `org.springframework.boot.context.event.ApplicationReadyEvent`: Used to hook into the application lifecycle.
*   `org.springframework.context.event.EventListener`: Annotation used to register the startup callback.
*   `org.springframework.stereotype.Component`: Marks the class as a Spring-managed bean.
*   `com.cloudbalancer.dispatcher.service.WorkerRegistryService`: The service dependency that performs the actual ledger rebuilding logic.

## Usage Notes

*   **Lifecycle Timing**: This component relies on the `ApplicationReadyEvent`, meaning it executes only after the application context is fully refreshed and the application is ready to accept requests.
*   **Initialization Behavior**: The primary purpose of this class is to prevent stale data in the resource ledger. By calling `rebuildResourceLedger` on startup, it ensures that any changes occurring while the dispatcher was offline are reconciled.
*   **Logging**: The class logs an info-level message ("Application ready — rebuilding resource ledger") to track the initialization sequence in the application logs.
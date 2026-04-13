# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/WorkerController.java

## Overview

The `WorkerController` is a Spring `@RestController` responsible for exposing worker-related information via the CloudBalancer dispatcher API. It provides a read-only interface to query the current state, health, and workload distribution of registered workers within the cluster.

This controller acts as an abstraction layer between the internal `WorkerRegistryService` and external clients, transforming internal `WorkerRecord` data into `WorkerSummary` DTOs for consumption.

## Public API

### `WorkerController`
The primary controller class mapped to the `/api/workers` endpoint.

*   **Constructor**: `WorkerController(WorkerRegistryService workerRegistryService)`
    *   Initializes the controller with the required `WorkerRegistryService` dependency via constructor injection.

*   **`listWorkers()`**
    *   **Endpoint**: `GET /api/workers`
    *   **Description**: Retrieves a list of all currently registered workers.
    *   **Returns**: `List<WorkerSummary>` containing the worker ID, health state, agent ID, active task count, and registration timestamp.

## Dependencies

*   **`com.cloudbalancer.dispatcher.service.WorkerRegistryService`**: The service layer component used to fetch the authoritative list of active workers.
*   **`com.cloudbalancer.dispatcher.api.dto.WorkerSummary`**: The Data Transfer Object used to structure the response payload.
*   **Spring Web MVC**: Utilizes `@RestController`, `@RequestMapping`, and `@GetMapping` annotations for request routing and serialization.

## Usage Notes

*   **Data Transformation**: The `listWorkers` method performs an on-the-fly mapping from internal domain objects to `WorkerSummary` DTOs. Note that the registration timestamp is converted to a `String` representation during this process.
*   **Health Monitoring**: The `healthState` field returned in the summary reflects the current status as determined by the `WorkerRegistryService`.
*   **Integration**: This controller is intended for use by administrative dashboards or monitoring tools that require visibility into the distribution of tasks across the worker pool.
*   **Call Flow**: When `listWorkers` is invoked, it calls `workerRegistryService.getAllWorkers()`. Each worker record's properties (such as `getAgentId()`, `getActiveTaskCount()`, etc.) are accessed to populate the summary list.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AdminController.java

## Overview

The `AdminController` is a Spring `@RestController` that provides administrative endpoints for the Cloud Balancer dispatcher. It acts as the primary interface for system operators to manage scheduling strategies, modify worker metadata (tags), and perform lifecycle management operations on individual worker nodes.

The controller is mapped to the `/api/admin` path and facilitates runtime configuration changes without requiring a system restart.

## Public API

### Endpoints

*   **`GET /api/admin/strategy`**
    *   Retrieves the currently active scheduling strategy and its associated weights.
    *   **Returns**: `StrategyResponse` containing the strategy name and weight mapping.

*   **`PUT /api/admin/strategy`**
    *   Updates the active scheduling strategy.
    *   **Parameters**: `StrategyRequest` (JSON body containing `strategy` name and optional `weights`).
    *   **Returns**: `200 OK` with the updated strategy details, or `400 Bad Request` if the strategy is invalid.

*   **`PUT /api/admin/workers/{id}/tags`**
    *   Updates the tags associated with a specific worker node.
    *   **Parameters**: `id` (path variable), `WorkerTagsRequest` (JSON body containing `tags`).
    *   **Returns**: `200 OK` with the updated set of tags, or `404 Not Found` if the worker does not exist.

*   **`DELETE /api/admin/workers/{id}`**
    *   Terminates a specific worker node.
    *   **Parameters**: `id` (path variable).
    *   **Returns**: `204 No Content` on success, `404 Not Found` if the worker is missing, or `409 Conflict` if the worker cannot be terminated in its current state.

## Dependencies

*   `com.cloudbalancer.common.runtime.NodeRuntime`: Used for executing the physical termination of worker nodes.
*   `com.cloudbalancer.dispatcher.service.SchedulingConfigService`: Manages the persistence and state of scheduling strategies.
*   `com.cloudbalancer.dispatcher.service.WorkerRegistryService`: Manages worker registration, metadata, and lifecycle state.
*   `org.springframework.web.bind.annotation.*`: Provides the RESTful routing annotations.

## Usage Notes

*   **Error Handling**: The controller maps specific domain exceptions to standard HTTP status codes. `IllegalArgumentException` is generally mapped to `400` or `404`, while `IllegalStateException` (specifically during worker termination) results in a `409 Conflict`.
*   **Strategy Weights**: When switching strategies, if the `weights` map is null in the request, the controller defaults to an empty map.
*   **Worker Lifecycle**: The `killWorker` operation is a two-step process: it first updates the registry state via `WorkerRegistryService` and then triggers the actual node shutdown via `NodeRuntime`. Ensure that the `NodeRuntime` implementation is correctly configured to communicate with the underlying infrastructure provider.
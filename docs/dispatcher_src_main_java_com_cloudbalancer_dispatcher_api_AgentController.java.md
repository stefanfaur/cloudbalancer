# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/AgentController.java

## Overview

The `AgentController` is a Spring `@RestController` that provides administrative endpoints for monitoring and managing compute agents within the `dispatcher` module. It acts as an interface between the system's internal `AgentRegistry` and `WorkerRepository` and external administrative clients, allowing users to query agent health, resource utilization, and associated worker status.

## Public API

The controller exposes the following REST endpoints under the `/api/admin/agents` base path:

*   **`GET /api/admin/agents`**: Returns a list of all currently active agents in the system.
*   **`GET /api/admin/agents/{agentId}`**: Retrieves detailed information for a specific agent identified by its ID. Returns `404 Not Found` if the agent does not exist.
*   **`GET /api/admin/agents/{agentId}/workers`**: Lists all workers currently associated with the specified agent, including their health state and task counts.

## Dependencies

The `AgentController` relies on the following components:

*   **`AgentRegistry`**: Used to retrieve the current state of active agents.
*   **`WorkerRepository`**: Used to query worker persistence data associated with specific agents.
*   **DTOs**:
    *   `AgentInfoResponse`: Data transfer object for agent status and resource metrics.
    *   `AgentWorkerResponse`: Data transfer object for individual worker status.

## Usage Notes

*   **Data Transformation**: The controller performs internal mapping from domain objects (`AgentInfo`) to API-specific DTOs (`AgentInfoResponse`) via the private `toResponse` method. This ensures that internal system details are decoupled from the public API contract.
*   **Error Handling**: The `get` method utilizes `Optional` handling to return a `404 Not Found` response if the requested `agentId` is not present in the `AgentRegistry`.
*   **Formatting**: Timestamps (such as `lastHeartbeat` and `registeredAt`) are converted to `String` representations during the DTO mapping process to ensure compatibility with JSON serialization.
*   **Integration**: This controller is intended for administrative use and assumes that callers have appropriate authorization to access the `/api/admin/**` path.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/ScalingController.java

## Overview

The `ScalingController` is a Spring `@RestController` that provides the primary HTTP interface for managing and monitoring the cloud balancer's scaling infrastructure. It exposes endpoints to retrieve the current scaling status, update scaling policies, and manually trigger scaling actions.

The controller acts as a bridge between external API requests and the internal service layer, coordinating operations between `ScalingPolicyService`, `WorkerRegistryService`, and `AutoScalerService`.

## Public API

### `ScalingController`
The constructor initializes the controller with the required service dependencies:
*   `ScalingPolicyService`: Manages policy configuration.
*   `WorkerRegistryService`: Tracks worker health and registration.
*   `AutoScalerService`: Executes scaling logic and tracks cooldowns.

### `getStatus()`
*   **Endpoint**: `GET /api/scaling/status`
*   **Description**: Returns a `ScalingStatusResponse` containing a snapshot of the current cluster state, including total worker counts, health states (active vs. draining), the active policy, the last scaling decision, and remaining cooldown time.

### `updatePolicy(ScalingPolicyRequest request)`
*   **Endpoint**: `PUT /api/scaling/policy`
*   **Description**: Updates the system's scaling policy. It validates the input parameters (min/max workers, cooldowns, and step sizes) using `ScalingPolicy.validated()`.
*   **Returns**: `200 OK` with the updated policy details, or `400 Bad Request` if validation fails.

### `triggerScaling(ScalingTriggerRequest request)`
*   **Endpoint**: `POST /api/scaling/trigger`
*   **Description**: Manually triggers a scaling action (e.g., scale up or down) for a specific number of instances.
*   **Returns**: `200 OK` with the updated `ScalingStatusResponse`, or `400 Bad Request` if the action type is invalid or the instance count is less than 1.

## Dependencies

*   **Spring Framework**: `org.springframework.web.bind.annotation`, `org.springframework.http.ResponseEntity`.
*   **Internal Models**: `com.cloudbalancer.common.model` (ScalingAction, ScalingPolicy, WorkerHealthState).
*   **Internal Services**:
    *   `com.cloudbalancer.dispatcher.service.AutoScalerService`
    *   `com.cloudbalancer.dispatcher.service.ScalingPolicyService`
    *   `com.cloudbalancer.dispatcher.service.WorkerRegistryService`
*   **DTOs**: `com.cloudbalancer.dispatcher.api.dto.*`

## Usage Notes

*   **Validation**: The `updatePolicy` method enforces strict validation. Ensure that `cooldownSeconds` and `drainTimeSeconds` are provided as positive durations to avoid `IllegalArgumentException`.
*   **Manual Scaling**: The `triggerScaling` endpoint is intended for administrative overrides. It bypasses standard automated logic but still requires a valid `agentId` and `ScalingAction` (e.g., `SCALE_UP`, `SCALE_DOWN`).
*   **Health States**: The `getStatus` endpoint categorizes workers into `HEALTHY`/`RECOVERING` (active) and `DRAINING` states. This is useful for monitoring the impact of scale-down operations.
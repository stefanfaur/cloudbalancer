# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/ChaosMonkeyController.java

## Overview

The `ChaosMonkeyController` is a Spring `@RestController` responsible for exposing administrative endpoints to trigger fault-injection scenarios within the `dispatcher` service. It acts as the entry point for chaos engineering operations, allowing operators to simulate system failures, worker instability, and network latency to test the resilience of the cloud balancer infrastructure.

This controller delegates all business logic to the `ChaosMonkeyService`.

## Public API

### `POST /api/admin/chaos/kill-worker`
Simulates the termination of a worker node.
- **Request Body**: Optional JSON object containing `workerId` (string). If omitted, the service may target a random worker.
- **Returns**: `ResponseEntity` containing the result of the termination request.

### `POST /api/admin/chaos/fail-task`
Forces a specific task to enter a failed state.
- **Request Body**: JSON object containing `taskId` (UUID string).
- **Returns**: `ResponseEntity` containing the result of the task failure injection.

### `POST /api/admin/chaos/latency`
Injects artificial network or processing latency into a specific system component.
- **Request Body**: JSON object containing:
    - `targetComponent` (string): The identifier of the component to impact.
    - `delayMs` (number): The duration of the delay in milliseconds.
    - `durationSeconds` (number): How long the latency injection should persist.
- **Returns**: `ResponseEntity` containing the confirmation of the latency injection.

## Dependencies

- **`com.cloudbalancer.dispatcher.service.ChaosMonkeyService`**: The core service layer that executes the fault injection logic.
- **Spring Web**: Utilized for REST endpoint mapping (`@RestController`, `@PostMapping`, `@RequestBody`).
- **Java Standard Library**: Uses `java.util.UUID` for task identification and `java.util.Optional` for safe request parameter handling.

## Usage Notes

- **Administrative Access**: These endpoints are located under `/api/admin/` and should be protected by appropriate security configurations (e.g., Spring Security) in production environments to prevent unauthorized system disruption.
- **Input Validation**: 
    - The `failTask` endpoint expects a valid UUID string; providing an malformed string will result in an `IllegalArgumentException` during parsing.
    - The `injectLatency` endpoint requires numeric values for `delayMs` and `durationSeconds`. Ensure the JSON payload correctly maps these to numeric types to avoid casting errors.
- **Chaos Engineering**: This controller is intended for use in testing and staging environments to validate circuit breakers and fault-tolerance mechanisms defined in `CircuitBreakerConfiguration`. Use with caution in production.
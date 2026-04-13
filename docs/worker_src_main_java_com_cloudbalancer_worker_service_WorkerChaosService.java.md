# File: worker/src/main/java/com/cloudbalancer/worker/service/WorkerChaosService.java

## Overview

`WorkerChaosService` is a specialized service within the `worker` module designed for fault injection testing. It allows developers to simulate network or processing delays by injecting artificial latency into specific components of the worker node for a defined duration. This service is instrumental in testing the resilience and timeout handling of the `cloudbalancer` system.

## Public API

### `injectLatency(String component, long delayMs, int durationSeconds)`
Configures a new latency injection event.
*   **component**: The identifier of the target component to affect.
*   **delayMs**: The duration of the delay to inject in milliseconds.
*   **durationSeconds**: How long the injection should remain active before expiring.

### `checkAndApplyLatency(String component)`
Checks if an active, non-expired latency injection exists for the specified component. If a match is found, it pauses the current thread for the configured delay duration.
*   **component**: The component identifier to check against the active injection.
*   **Throws**: `InterruptedException` if the thread is interrupted while sleeping.

### `isExpired()` (Internal to `LatencyInjection` record)
Determines if the current `LatencyInjection` configuration has passed its `expiresAt` timestamp.

## Dependencies

*   `java.time.Instant`: Used for tracking expiration timestamps.
*   `java.util.concurrent.atomic.AtomicReference`: Ensures thread-safe updates to the active latency configuration.
*   `org.slf4j.Logger`: Used for logging chaos injection events and application status.
*   `org.springframework.stereotype.Service`: Marks the class as a Spring-managed bean.

## Usage Notes

*   **Thread Safety**: The service uses an `AtomicReference` to store the `LatencyInjection` state, making it safe for concurrent access by multiple worker threads.
*   **Cleanup**: The `checkAndApplyLatency` method performs "lazy cleanup." If it detects an expired injection during a check, it will automatically reset the `latencyInjection` reference to `null`.
*   **Performance Impact**: Because `checkAndApplyLatency` utilizes `Thread.sleep()`, it will block the calling thread. Use this service carefully in production environments, as it is intended primarily for controlled chaos engineering and testing scenarios.
*   **Granularity**: Latency is applied per component. Ensure the `component` string passed to `injectLatency` matches the string passed to `checkAndApplyLatency` exactly to ensure the delay is applied correctly.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/util/BackoffCalculator.java

## Overview

The `BackoffCalculator` is a utility class designed to compute retry intervals for failed operations within the `cloudbalancer` system. It provides a centralized mechanism to determine the next scheduled retry time based on configurable backoff strategies, ensuring that system resources are not overwhelmed during periods of instability.

## Public API

### `BackoffCalculator`

The class provides a static utility method to calculate retry timestamps.

#### `calculateNextRetryTime(BackoffStrategy strategy, int attemptNumber, long baseDelaySeconds, Instant now)`

Calculates the `Instant` at which the next retry should occur.

*   **Parameters:**
    *   `strategy` (`BackoffStrategy`): The algorithm used to determine the delay (FIXED, EXPONENTIAL, or EXPONENTIAL_WITH_JITTER).
    *   `attemptNumber` (`int`): The current attempt count (1-indexed).
    *   `baseDelaySeconds` (`long`): The initial delay duration in seconds.
    *   `now` (`Instant`): The reference point in time from which the delay is calculated.
*   **Returns:** `Instant` representing the calculated future retry time.

## Dependencies

*   `com.cloudbalancer.common.model.BackoffStrategy`: Defines the supported backoff algorithms.
*   `java.time.Instant`: Used for precise time calculations.
*   `java.util.concurrent.ThreadLocalRandom`: Used for generating jitter in retry intervals to prevent thundering herd problems.

## Usage Notes

*   **Strategy Behavior**:
    *   `FIXED`: Returns `now + baseDelaySeconds`.
    *   `EXPONENTIAL`: Calculates delay as `baseDelaySeconds * 2^(attemptNumber - 1)`.
    *   `EXPONENTIAL_WITH_JITTER`: Applies exponential backoff and adds a random jitter component (0 to `baseDelaySeconds` seconds) to distribute retry load.
*   **Thread Safety**: This class is stateless and thread-safe, as it relies on `ThreadLocalRandom` for concurrent-safe random number generation.
*   **Implementation Note**: Ensure the `attemptNumber` passed is accurate, as it directly influences the exponent in exponential strategies.
# File: common/src/main/java/com/cloudbalancer/common/model/BackoffStrategy.java

## Overview

`BackoffStrategy` is an enumeration that defines the supported algorithms for calculating retry intervals in the `cloudbalancer` system. It is used to configure how the system handles delays between successive attempts when an operation fails, allowing for flexible traffic shaping and load management during error recovery.

## Public API

The `BackoffStrategy` enum provides the following constants:

*   **`FIXED`**: Implements a constant delay between retry attempts.
*   **`EXPONENTIAL`**: Implements an exponential growth pattern for delay intervals (e.g., $delay = base \times 2^{attempt}$).
*   **`EXPONENTIAL_WITH_JITTER`**: Implements an exponential growth pattern with added randomness (jitter) to prevent "thundering herd" problems where multiple clients retry simultaneously.

## Dependencies

This enum is a standalone component within the `com.cloudbalancer.common.model` package and does not depend on any external libraries or other internal classes.

## Usage Notes

*   **Integration**: This enum is primarily consumed by the `BackoffCalculator` utility class located in the `dispatcher` module to determine the specific mathematical approach for calculating wait times.
*   **Configuration**: It is often used as a field within `ExecutionPolicy` to define the retry behavior for specific task execution configurations.
*   **Best Practices**: Use `EXPONENTIAL_WITH_JITTER` in distributed environments to reduce contention on backend services during periods of high failure rates.
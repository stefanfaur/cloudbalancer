# File: common/src/main/java/com/cloudbalancer/common/model/ExecutionAttempt.java

## Overview

The `ExecutionAttempt` class is a Java `record` that serves as a data model for tracking the lifecycle and outcome of a specific task execution attempt within the CloudBalancer system. It encapsulates metadata regarding when an execution occurred, which worker performed it, the resource consumption, and the final status of the operation.

## Public API

The `ExecutionAttempt` record provides the following immutable fields:

*   **`attemptNumber` (int)**: The sequential index of the attempt for a given execution.
*   **`workerId` (String)**: The unique identifier of the worker node that processed the task.
*   **`startedAt` (Instant)**: The timestamp marking the beginning of the execution.
*   **`completedAt` (Instant)**: The timestamp marking the conclusion of the execution.
*   **`exitCode` (int)**: The process exit status code returned upon completion.
*   **`actualResources` (ResourceProfile)**: The specific resource profile consumed during this attempt.
*   **`failureReason` (String)**: A descriptive message explaining why the attempt failed, if applicable.
*   **`workerCausedFailure` (boolean)**: A flag indicating if the failure was attributed to the worker node rather than the task itself.
*   **`executionId` (UUID)**: The unique identifier linking this attempt to a parent execution.

## Dependencies

*   `java.time.Instant`: Used for precise timestamping of execution events.
*   `java.util.UUID`: Used for unique identification of the parent execution context.
*   `com.cloudbalancer.common.model.ResourceProfile`: Referenced as the type for `actualResources`.

## Usage Notes

*   **Immutability**: As a Java `record`, all fields are final and immutable. Once an `ExecutionAttempt` object is instantiated, its state cannot be modified.
*   **Data Integrity**: This model is primarily used for logging, auditing, and reporting purposes within the task scheduling and load balancing pipeline.
*   **Failure Analysis**: The `workerCausedFailure` boolean is critical for the system's health monitoring, allowing the balancer to distinguish between transient task errors and infrastructure-level failures.
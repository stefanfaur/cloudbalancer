# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/SchedulingConfig.java

## Overview

The `SchedulingConfig` class is a Spring `@Configuration` component responsible for defining and initializing the scheduling strategy components for the `dispatcher` module. It centralizes the instantiation of worker filtering and scoring logic, ensuring that the scheduling engine has access to the necessary rules for selecting and ranking worker nodes.

## Public API

### `SchedulingConfig`
The main configuration class that registers beans required for the task scheduling lifecycle.

### `workerFilters()`
*   **Return Type**: `List<WorkerFilter>`
*   **Description**: Defines the chain of responsibility for filtering out ineligible workers. The current implementation includes:
    *   `HealthFilter`: Ensures the worker is active and responsive.
    *   `ExecutorCapabilityFilter`: Verifies the worker supports the required execution environment.
    *   `ResourceSufficiencyFilter`: Checks if the worker has enough available hardware resources.
    *   `ConstraintFilter`: Validates any specific placement constraints or affinity rules.

### `workerScorers()`
*   **Return Type**: `List<WorkerScorer>`
*   **Description**: Defines the collection of scoring algorithms used to rank eligible workers. The current implementation includes:
    *   `ResourceAvailabilityScorer`: Ranks workers based on current resource overhead.
    *   `QueueDepthScorer`: Ranks workers based on the length of their pending task queues.

## Dependencies

*   `org.springframework.context.annotation.Bean`: Used for bean registration within the Spring context.
*   `org.springframework.context.annotation.Configuration`: Marks the class as a source of bean definitions.
*   `java.util.List`: Used for managing collections of filter and scorer implementations.

## Usage Notes

*   **Extensibility**: To add new filtering or scoring logic, update the respective `List.of(...)` definitions within this class. The scheduling engine will automatically pick up the updated lists via dependency injection.
*   **Order of Execution**: The order of elements in the `workerFilters` list defines the sequence in which filters are applied. It is recommended to place the most restrictive or computationally inexpensive filters first to optimize scheduling performance.
*   **Integration**: This configuration is consumed by the `SchedulingConfigService` to orchestrate the load-balancing strategy across the `CloudBalancer` infrastructure.
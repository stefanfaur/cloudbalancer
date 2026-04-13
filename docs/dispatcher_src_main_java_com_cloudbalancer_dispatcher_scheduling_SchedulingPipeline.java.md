# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/scheduling/SchedulingPipeline.java

## Overview

The `SchedulingPipeline` class is a core component of the `dispatcher` module, responsible for orchestrating the selection of an optimal `WorkerRecord` for a given `TaskRecord`. It implements a multi-stage decision process that combines filtering and scoring logic to ensure tasks are assigned to the most suitable workers based on current system configuration.

The pipeline operates in two primary stages:
1.  **Filtering**: Sequential application of `WorkerFilter` implementations to prune the list of candidate workers.
2.  **Scoring and Selection**: Execution of a dynamic `SchedulingStrategy` (retrieved via `SchedulingConfigService`) that utilizes registered `WorkerScorer` components to rank and select the final worker.

## Public API

### `SchedulingPipeline(List<WorkerFilter> filters, List<WorkerScorer> scorerList, SchedulingConfigService configService)`
Constructs a new `SchedulingPipeline`.
*   **filters**: A list of filters to be applied to candidate workers.
*   **scorerList**: A list of available scoring components, which are indexed by their name for dynamic retrieval during the selection phase.
*   **configService**: The service used to retrieve the active `SchedulingStrategy`.

### `Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates)`
Processes a task against a list of candidate workers to determine the optimal assignment.
*   **task**: The `TaskRecord` requiring assignment.
*   **candidates**: A list of available `WorkerRecord` instances.
*   **Returns**: An `Optional` containing the selected `WorkerRecord`, or `Optional.empty()` if no worker satisfies the filtering criteria or the strategy fails to select one.

## Dependencies

*   **`com.cloudbalancer.dispatcher.persistence.TaskRecord`**: Data model representing the task to be scheduled.
*   **`com.cloudbalancer.dispatcher.persistence.WorkerRecord`**: Data model representing the worker node.
*   **`com.cloudbalancer.dispatcher.service.SchedulingConfigService`**: Provides access to the current scheduling configuration and active strategy.
*   **`WorkerFilter`**: Interface for defining exclusion criteria for workers.
*   **`WorkerScorer`**: Interface for defining evaluation logic for workers.
*   **`SchedulingStrategy`**: Interface for defining the final selection logic based on scores.

## Usage Notes

*   **Pipeline Execution**: The pipeline is designed to be fail-fast during the filtering stage. If any filter returns an empty list, the pipeline immediately terminates and returns `Optional.empty()`, logging the event at the `DEBUG` level.
*   **Strategy Dynamism**: The selection logic is not hardcoded; it relies on the `SchedulingConfigService`. This allows the system to switch between different scheduling algorithms (e.g., Round Robin, Least Loaded, Affinity-based) at runtime without modifying the pipeline code.
*   **Scorer Mapping**: Upon initialization, the `scorerList` is converted into a `Map<String, WorkerScorer>` for efficient lookup by the active `SchedulingStrategy`. Ensure that all required scorers are provided during the construction of the bean.
*   **Logging**: The class uses `SLF4J` for logging. It provides visibility into the filtering process and the final selection outcome, which is useful for auditing task placement decisions.
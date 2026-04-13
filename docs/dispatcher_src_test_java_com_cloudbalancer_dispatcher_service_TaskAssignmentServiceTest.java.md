# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/TaskAssignmentServiceTest.java

## Overview

`TaskAssignmentServiceTest` is a critical integration test suite for the `TaskAssignmentService` within the `dispatcher` module. It validates the scheduling logic that matches pending tasks to available worker nodes based on resource availability, executor compatibility, and task priority.

**Warning**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As the primary gatekeeper for task scheduling logic, any regressions here can lead to cluster-wide task starvation, resource over-allocation, or incorrect task distribution.

## Public API

The class is a test suite and does not expose a public API for production use. It utilizes the following internal helper methods to facilitate test setup:

*   **`registerWorker(String id, Set<ExecutorType> executors, int cpu, int mem, int disk)`**: Registers a worker with specific capabilities and resource profiles into the `WorkerRegistryService`.
*   **`submitTask(Priority priority, ExecutorType executorType, int cpu, int mem, int disk)`**: Injects a new task into the `TaskService` with defined resource requirements and priority levels.

## Dependencies

This test suite relies on the following components:

*   **Spring Boot Test**: Uses `@SpringBootTest` and `TestContainersConfig` to provide a real database environment (via Testcontainers) for persistence testing.
*   **Persistence Layer**: `TaskRepository` and `WorkerRepository` for state verification.
*   **Service Layer**: `TaskAssignmentService` (the System Under Test), `TaskService`, and `WorkerRegistryService`.
*   **Domain Models**: `com.cloudbalancer.common.model.*` (e.g., `TaskState`, `ExecutorType`, `Priority`, `ResourceProfile`).

## Usage Notes

### Testing Strategy
The suite employs a "Setup-Execute-Verify" pattern:
1.  **Setup**: Use `registerWorker` and `submitTask` to populate the state.
2.  **Execute**: Invoke `assignmentService.assignPendingTasks()`.
3.  **Verify**: Assert against `taskRepository` and `workerRepository` to confirm the expected state transitions (e.g., `QUEUED` to `ASSIGNED`) and resource ledger updates.

### Key Scenarios Covered
*   **Priority Precedence**: Validates that `CRITICAL` tasks are processed before `LOW` priority tasks when resources are constrained.
*   **Resource Ledger Integrity**: Ensures that when a task is assigned, the worker's `allocatedCpu`, `allocatedMemoryMb`, and `allocatedDiskMb` are accurately incremented.
*   **Filtering Logic**:
    *   **Compatibility**: Tasks are not assigned if the worker does not support the required `ExecutorType`.
    *   **Health Checks**: Tasks are not assigned to workers marked with `WorkerHealthState.DEAD`.
    *   **Availability**: Tasks remain `QUEUED` if no workers are registered or if all registered workers lack sufficient capacity.

### Potential Pitfalls
*   **Database State**: The `@BeforeEach` method calls `deleteAll()` on both repositories. If tests are run in parallel or if global state is introduced, this may cause intermittent failures.
*   **Resource Constraints**: When adding new tests, ensure the `ResourceProfile` values in `registerWorker` and `submitTask` are mathematically consistent; otherwise, tests may fail due to unexpected resource exhaustion rather than logic errors.
*   **Testcontainer Latency**: Because this test uses `TestContainersConfig`, execution time is higher than unit tests. Avoid adding excessive test cases to this file; consider moving logic-only tests to a pure unit test class if possible.
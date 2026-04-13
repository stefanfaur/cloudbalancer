# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/integration/Phase4SchedulingIntegrationTest.java

## Overview

`Phase4SchedulingIntegrationTest` is a critical integration test suite for the `dispatcher` module of the CloudBalancer system. It serves as the primary validation layer for the task scheduling engine, ensuring that tasks are correctly routed to workers based on health, capability, resource availability, and constraints.

**Status**: This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As the central integration point for scheduling logic, modifications here carry a high risk of introducing regressions in task distribution and resource management.

## Public API

The class provides a comprehensive set of test cases that exercise the `TaskAssignmentService`, `WorkerRegistryService`, and `TaskService`.

### Key Test Methods
*   **`healthFilterExcludesDeadWorkers`**: Verifies that the scheduler ignores workers in a `DEAD` state.
*   **`executorCapabilityFilterExcludesIncompatibleWorkers`**: Ensures tasks are only assigned to workers supporting the required `ExecutorType`.
*   **`resourceSufficiencyFilterExcludesOverloadedWorkers`**: Validates that the scheduler respects resource limits (CPU, Memory, Disk) when selecting a worker.
*   **`constraintFilterEnforcesRequiredTags`**: Confirms that task-specific constraints (e.g., "gpu-enabled") are honored during selection.
*   **`priorityOrderingCriticalBeforeLow`**: Validates the priority queue logic, ensuring `CRITICAL` tasks are scheduled before `LOW` priority tasks.
*   **`resourceLedgerTracksAcrossLifecycle`**: Checks that the internal resource ledger correctly increments upon assignment and decrements upon task completion.
*   **`strategySwitchViaAdminApi`**: Tests the dynamic switching of scheduling strategies (e.g., `ROUND_ROBIN` vs `RESOURCE_FIT`) via the Admin REST API.
*   **`fullLifecycleWithKafkaAndPostgres`**: An end-to-end test simulating a real task lifecycle, including Kafka message passing and Postgres persistence.

## Dependencies

This test suite relies on the following infrastructure and services:
*   **Spring Boot Test**: Provides the application context and `RandomPort` web environment.
*   **TestContainers**: Orchestrates ephemeral `Kafka` and `Postgres` instances for integration testing.
*   **TaskRepository / WorkerRepository**: Used for direct database verification of state transitions.
*   **TaskAssignmentService**: The primary unit under test for scheduling logic.
*   **RestClient**: Used to interact with the system's Admin and Task APIs.

## Usage Notes

### Test Environment Setup
The suite uses `TestContainersConfig` to spin up necessary infrastructure. Ensure that Docker is running in the environment where these tests are executed.

### Adding New Scheduling Rules
When adding new scheduling filters or strategies, follow the pattern established in this class:
1.  **Register Workers**: Use the helper methods `registerWorker` or `registerWorkerWithTags` to set up the desired state.
2.  **Submit Tasks**: Use `submitTask` to inject the workload.
3.  **Trigger Assignment**: Call `assignmentService.assignPendingTasks()` to execute the scheduling logic.
4.  **Assert State**: Use `taskRepository` or `workerRepository` to verify the expected outcome.

### Troubleshooting Lifecycle Tests
The `fullLifecycleWithKafkaAndPostgres` test is sensitive to timing. If this test fails intermittently:
*   Check the `await()` configuration; the default timeout is 30 seconds.
*   Ensure the `TestWorkerSimulator` is correctly cleaning up resources in the `finally` block to prevent port conflicts or thread leaks.

### Example: Verifying a New Constraint
To test a new worker constraint, add a test case following this structure:
```java
@Test
void myNewConstraintTest() {
    registerWorkerWithTags("w1", Set.of("special-feature"));
    submitTaskWithConstraint("special-feature");
    
    assignmentService.assignPendingTasks();
    
    var assigned = taskRepository.findByState(TaskState.ASSIGNED);
    assertThat(assigned.getFirst().getAssignedWorkerId()).isEqualTo("w1");
}
```
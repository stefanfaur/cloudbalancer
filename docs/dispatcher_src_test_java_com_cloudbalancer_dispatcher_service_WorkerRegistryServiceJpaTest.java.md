# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/WorkerRegistryServiceJpaTest.java

## Overview

`WorkerRegistryServiceJpaTest` is a critical integration test suite for the `WorkerRegistryService`. It validates the interaction between the service layer and the underlying persistence layer (`WorkerRepository` and `TaskRepository`). 

**Note:** This file is a **HOTSPOT** within the `dispatcher` module, ranking in the top 25% for both change frequency and complexity. It is a high-risk area for bugs, as it governs the state machine of worker nodes and resource allocation logic. Changes to this test suite should be treated with extreme caution, as they often reflect fundamental shifts in how the system tracks worker health and resource consumption.

## Public API

The test class does not expose a public API but provides a suite of test cases that verify the following service behaviors:

- **`registerAndRetrieveWorker`**: Ensures workers can be registered with specific capabilities and retrieved correctly.
- **`allocateAndReleaseResources`**: Validates that resource accounting (CPU, memory, disk) is correctly updated in the database during task assignment and release.
- **`rebuildLedgerFromPersistedTasks`**: Verifies the system's ability to reconstruct the resource ledger by scanning existing `TaskRecord` entries in the database.
- **`getAvailableWorkersReturnsOnlyHealthy`**: Confirms that the registry filters out workers that are not in a `HEALTHY` state.
- **`getAvailableWorkersExcludesDraining`**: Ensures that workers marked as `DRAINING` are excluded from scheduling availability.
- **`drainWorkerTransitionsToDraining`**: Validates the state transition logic when a worker is put into a draining state.

## Dependencies

The test suite relies on the following components:

- **Spring Boot Test**: Uses `@SpringBootTest` and `@Import(TestContainersConfig.class)` to provide a full application context with a containerized database.
- **Persistence Layer**: Directly interacts with `WorkerRepository` and `TaskRepository` to verify state changes.
- **Domain Models**: Utilizes `WorkerCapabilities`, `ResourceProfile`, `TaskDescriptor`, and `TaskRecord` to simulate real-world worker and task scenarios.
- **JUnit 5**: Standard testing framework for lifecycle management (`@BeforeEach`).

## Usage Notes

### Testing Lifecycle
The `cleanUp()` method annotated with `@BeforeEach` ensures that the database is wiped before every test case. This prevents state leakage between tests, which is essential given the high-risk nature of the resource allocation logic.

### Resource Ledger Reconstruction
The `rebuildLedgerFromPersistedTasks` test is particularly important. It simulates a system crash or restart scenario where the in-memory resource ledger is lost. Developers modifying the `WorkerRegistryService` should ensure that any new resource types added to the system are also accounted for in the `rebuildResourceLedger` logic.

### Example: Adding a New Test Case
When adding a new test case to verify a state transition, follow the pattern established in `drainWorkerTransitionsToDraining`:

1. **Setup**: Use `registerWorker(...)` to create the initial state.
2. **Action**: Invoke the target method on `workerRegistry`.
3. **Verification**: Use `workerRepository.findById(...)` to fetch the entity and use AssertJ to verify the updated state:
   ```java
   @Test
   void customStateTransitionTest() {
       registerWorker("w1", 4, 2048, 500);
       workerRegistry.someNewAction("w1");
       var w = workerRepository.findById("w1").orElseThrow();
       assertThat(w.getSomeField()).isEqualTo(expectedValue);
   }
   ```

### Pitfalls
- **Database Constraints**: Because this is a JPA test, ensure that any changes to the `WorkerRecord` or `TaskRecord` entities are reflected in the database schema; otherwise, tests will fail during the `save()` operations.
- **TestContainers**: Since this uses `TestContainersConfig`, ensure your development environment has Docker running, or the tests will fail to initialize the database context.
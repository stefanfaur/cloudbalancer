# File: worker/src/test/java/com/cloudbalancer/worker/config/ExecutorConfigTest.java

## Overview

`ExecutorConfigTest` is a comprehensive JUnit 5 test suite responsible for validating the factory logic within `ExecutorConfig`. It ensures that the worker module correctly instantiates the appropriate `TaskExecutor` implementations based on the provided `WorkerProperties`.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for bugs, as changes to the executor instantiation logic directly impact the worker's ability to process tasks. Any modifications to `ExecutorConfig` should be strictly verified against these tests to prevent regressions in task execution capabilities.

## Public API

The class contains the following test methods which serve as the validation suite for the `ExecutorConfig` component:

*   `whenSimulatedConfigured_thenSimulatedExecutorCreated()`: Verifies that `SimulatedExecutor` is instantiated when configured.
*   `whenShellConfigured_thenShellExecutorCreatedWithConfigFromProperties()`: Validates that `ShellExecutor` is created and correctly inherits configuration (e.g., blocked commands, output limits) from `WorkerProperties`.
*   `whenOnlySimulatedConfigured_thenNoShellOrDockerExecutors()`: Ensures strict isolation; verifies that only the requested executor type is present.
*   `whenMultipleExecutorsConfigured_thenAllCreated()`: Confirms that multiple executor types can coexist and be initialized simultaneously.
*   `whenDockerConfigured_thenDockerExecutorCreated()`: Verifies `DockerExecutor` instantiation.
*   `whenPythonConfigured_thenPythonExecutorCreated()`: Verifies `PythonExecutor` instantiation.
*   `executorListMatchesConfiguredTypes()`: Validates that the list of initialized executors matches the `ExecutorType` set defined in properties.

## Dependencies

The test suite relies on the following internal and external components:

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management (`@BeforeEach`, `@Test`).
*   **AssertJ**: Used for fluent assertions (`assertThat`).
*   **Common Module**: Depends on `com.cloudbalancer.common.executor.*` (the various `TaskExecutor` implementations) and `com.cloudbalancer.common.model.ExecutorType`.
*   **Worker Module**: Depends on `com.cloudbalancer.worker.config.ExecutorConfig` and `WorkerProperties`.

## Usage Notes

### Testing Strategy
The tests follow a "Given-When-Then" pattern. The `buildProperties(Set<ExecutorType>)` helper method is the primary utility for setting up test scenarios.

### Adding New Executor Types
If a new `ExecutorType` is added to the system:
1.  Update `ExecutorConfig` to handle the new type.
2.  Add a corresponding test case in `ExecutorConfigTest` following the pattern of `when[Type]Configured_then[Type]ExecutorCreated`.
3.  Ensure that the `executorListMatchesConfiguredTypes` test is updated if the new type introduces unique configuration requirements.

### Common Pitfalls
*   **Configuration Mismatch**: When testing `ShellExecutor`, ensure that the `WorkerProperties` object is fully populated with the necessary sub-configurations (e.g., `props.getShell().setBlockedCommands(...)`), otherwise, the test may fail due to `NullPointerException` if the config object is not initialized.
*   **Hotspot Sensitivity**: Because this file is a hotspot, avoid refactoring the test logic without verifying the underlying `ExecutorConfig` implementation. If tests fail, it often indicates that the factory logic in `ExecutorConfig` has become misaligned with the expected `WorkerProperties` schema.

### Example: Adding a Test Case
To test a new executor type (e.g., `CustomExecutor`):
```java
@Test
void whenCustomConfigured_thenCustomExecutorCreated() {
    WorkerProperties props = buildProperties(Set.of(ExecutorType.CUSTOM));
    List<TaskExecutor> executors = executorConfig.taskExecutors(props);
    
    assertThat(executors).hasSize(1);
    assertThat(executors.get(0)).isInstanceOf(CustomExecutor.class);
}
```
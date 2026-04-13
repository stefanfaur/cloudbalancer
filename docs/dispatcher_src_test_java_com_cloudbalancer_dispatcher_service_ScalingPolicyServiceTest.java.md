# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/service/ScalingPolicyServiceTest.java

## Overview

`ScalingPolicyServiceTest` is an integration test suite for the `ScalingPolicyService`. It validates the persistence and retrieval logic of auto-scaling configurations within the `dispatcher` module. The tests ensure that the service correctly handles default values when no database records exist and confirms that updates to scaling policies are correctly persisted to the underlying repository.

## Public API

### `ScalingPolicyServiceTest`

*   **`cleanUp()`**: Annotated with `@BeforeEach`, this method clears the `ScalingPolicyRepository` and triggers a cache reload in `ScalingPolicyService` to ensure test isolation.
*   **`returnsDefaultsWhenNoDbRow()`**: Verifies that the service returns the expected hardcoded default values (min: 2, max: 20) when the database is empty.
*   **`updatePolicyPersistsAndReturns()`**: Tests the `updatePolicy` workflow by injecting a custom `ScalingPolicy` object and verifying that subsequent calls to `getCurrentPolicy` return the updated values.

## Dependencies

*   **`com.cloudbalancer.common.model.ScalingPolicy`**: Data model representing the scaling configuration.
*   **`com.cloudbalancer.dispatcher.persistence.ScalingPolicyRepository`**: Persistence layer interface used to manage policy state in the database.
*   **`com.cloudbalancer.dispatcher.test.TestContainersConfig`**: Provides containerized infrastructure (e.g., Testcontainers) required for integration testing.
*   **Spring Boot Test**: Utilizes `@SpringBootTest` for full application context loading.
*   **AssertJ**: Used for fluent assertion syntax.

## Usage Notes

*   **Environment**: This test requires a running database instance, provided via `TestContainersConfig`. Ensure that Docker is available in the environment where these tests are executed.
*   **State Management**: Because the service caches policy data, the `cleanUp` method is essential. Any new test cases added to this suite should ensure they do not leave stale state in the `ScalingPolicyService` cache.
*   **Integration**: This test suite serves as a verification point for the interaction between the `ScalingPolicyService` and the database, ensuring that the persistence layer correctly reflects the business logic defined in the service.
# File: dispatcher/src/test/java/com/cloudbalancer/dispatcher/persistence/WorkerRecordRepositoryTest.java

## Overview

`WorkerRecordRepositoryTest` is a JUnit 5 integration test suite designed to verify the persistence layer logic for `WorkerRecord` entities. It ensures that the `WorkerRepository` correctly handles CRUD operations, state-based filtering, and resource ledger management within the `dispatcher` module.

The test suite utilizes `TestContainersConfig` to provide a real database environment, ensuring that JPA mappings, capability serialization, and repository queries function as expected in a production-like setting.

## Public API

The test class does not expose a public API as it is a testing component. However, it validates the following repository and entity behaviors:

*   **`WorkerRepository`**: Validates persistence, retrieval by ID, and filtering by `WorkerHealthState`.
*   **`WorkerRecord`**: Validates the internal resource ledger logic, specifically the `allocateResources` and `releaseResources` methods.

## Dependencies

*   **Spring Boot Test**: Provides the `@SpringBootTest` context and `@Autowired` injection.
*   **Testcontainers**: Used via `TestContainersConfig` to spin up ephemeral database instances.
*   **AssertJ**: Used for fluent assertion syntax.
*   **Domain Models**: Relies on `WorkerCapabilities`, `ResourceProfile`, `WorkerHealthState`, and `ExecutorType` from the `common` module.

## Usage Notes

*   **Environment**: This test requires a running Docker daemon to initialize the database container defined in `TestContainersConfig`.
*   **Cleanup**: The `@BeforeEach` method (`cleanUp`) ensures that the `workerRepository` is cleared before every test case to maintain test isolation and prevent state leakage.
*   **Resource Ledger Testing**: The `resourceLedgerAllocateAndRelease` test specifically verifies that the `WorkerRecord` entity correctly tracks allocated CPU, memory, disk, and active task counts, which is critical for the dispatcher's scheduling logic.
*   **Helper Methods**: The `minCaps()` method is a private utility used to generate standard `WorkerCapabilities` objects for test setup, reducing boilerplate code in individual test cases.
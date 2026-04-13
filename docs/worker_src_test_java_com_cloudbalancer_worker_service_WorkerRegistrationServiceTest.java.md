# File: worker/src/test/java/com/cloudbalancer/worker/service/WorkerRegistrationServiceTest.java

## Overview

`WorkerRegistrationServiceTest` is a JUnit 5 test class designed to validate the registration logic of the `WorkerRegistrationService`. It ensures that when a worker initializes, it correctly publishes a `WorkerRegisteredEvent` to the designated Kafka topic with the appropriate configuration and resource capabilities.

## Public API

### `WorkerRegistrationServiceTest`
The test class utilizes `MockitoExtension` to mock the `KafkaTemplate` dependency, allowing for isolated verification of the event publishing mechanism.

### `registerPublishesWorkerRegisteredEvent()`
This test method performs the following operations:
1.  **Configuration**: Initializes a `WorkerProperties` object with mock data (ID, executors, CPU, memory, disk).
2.  **Execution**: Invokes the `register()` method on the `WorkerRegistrationService`.
3.  **Verification**: 
    *   Verifies that `kafkaTemplate.send()` is called with the correct topic (`workers.registration`) and key (`test-worker`).
    *   Captures the serialized JSON payload sent to Kafka.
    *   Deserializes the payload back into a `WorkerRegisteredEvent` using `JsonUtil`.
    *   Asserts that the event contents match the input properties.

## Dependencies

*   **JUnit 5**: Used for test lifecycle management (`@Test`, `@ExtendWith`).
*   **Mockito**: Used for mocking `KafkaTemplate` and capturing arguments.
*   **AssertJ**: Used for fluent assertions on the event object.
*   **CloudBalancer Common**:
    *   `WorkerRegisteredEvent`: The event schema being validated.
    *   `JsonUtil`: Used for JSON serialization/deserialization during verification.
    *   `ExecutorType`, `ResourceProfile`, `WorkerCapabilities`: Domain models used to construct the test data.
*   **Spring Kafka**: Provides the `KafkaTemplate` interface being tested.

## Usage Notes

*   **Mocking**: The test relies on `Mockito` to intercept Kafka interactions. If the `WorkerRegistrationService` logic changes to include additional Kafka headers or different topics, the `verify` statement in `registerPublishesWorkerRegisteredEvent` must be updated accordingly.
*   **Serialization**: The test implicitly verifies that the `WorkerRegisteredEvent` is compatible with the `JsonUtil` configuration. Any changes to the `WorkerRegisteredEvent` class structure should be reflected here to ensure backward compatibility or correct serialization.
*   **Environment**: This test is strictly a unit test and does not require a running Kafka broker or Spring context, making it suitable for fast execution in CI/CD pipelines.
# File: common/src/test/java/com/cloudbalancer/common/event/KafkaEventIntegrationTest.java

## Overview

`KafkaEventIntegrationTest` is a critical integration test suite that validates the end-to-end messaging lifecycle for the CloudBalancer system. It ensures that `CloudBalancerEvent` subclasses (such as `TaskSubmittedEvent` and `WorkerRegisteredEvent`) are correctly serialized to JSON, transmitted through an Apache Kafka broker, and deserialized back into their original object types.

**Note:** This file is identified as a **HOTSPOT** (top 25% for change frequency and complexity). It is a high-risk area for bugs, as changes to event schemas or serialization logic directly impact the reliability of the system's event-driven architecture.

## Public API

The class provides test methods that act as verification points for the Kafka messaging infrastructure:

*   **`produceAndConsumeTaskSubmittedEvent()`**: Verifies that a `TaskSubmittedEvent` can be successfully sent to and retrieved from a Kafka topic. It checks for correct field mapping, specifically ensuring the `taskId` and `executorType` are preserved during the round-trip.
*   **`produceAndConsumeWorkerRegisteredEvent()`**: Verifies the transmission of `WorkerRegisteredEvent`. It confirms that complex nested objects like `WorkerCapabilities` and `ResourceProfile` maintain their integrity after deserialization.

### Internal Helper Methods
*   **`createProducer(String bootstrapServers)`**: Configures and returns a `KafkaProducer` using `StringSerializer` for both keys and values.
*   **`createConsumer(String bootstrapServers)`**: Configures and returns a `KafkaConsumer` using `StringDeserializer`. It automatically generates a unique `group.id` to ensure isolation between test runs and sets `auto.offset.reset` to `earliest` to ensure all messages are captured.

## Dependencies

*   **`KafkaTestContainer`**: Provides the ephemeral Kafka broker environment required for the tests.
*   **`JsonUtil`**: Used to retrieve the configured `ObjectMapper` for consistent JSON serialization/deserialization.
*   **`com.cloudbalancer.common.model.*`**: Contains the domain models (`TaskSubmittedEvent`, `WorkerRegisteredEvent`, etc.) being tested.
*   **Apache Kafka Clients**: Standard Kafka client libraries for producer/consumer interactions.
*   **JUnit 5 & AssertJ**: Used for test execution and fluent assertion validation.

## Usage Notes

### Testing Lifecycle
1.  **Environment Setup**: The tests rely on `KafkaTestContainer` to spin up a Dockerized Kafka instance. Ensure that the environment running these tests has Docker support enabled.
2.  **Serialization Consistency**: These tests enforce that any changes to the `CloudBalancerEvent` class hierarchy must remain compatible with the `JsonUtil` configuration. If you add a new event type, you must update the `CloudBalancerEvent` polymorphic deserialization configuration (usually handled by Jackson annotations).
3.  **Timeout Considerations**: The `consumer.poll(Duration.ofSeconds(10))` call is configured with a generous timeout. If tests are failing in CI/CD, ensure the Kafka container has sufficient resources to initialize within the allotted time.

### Common Pitfalls
*   **Topic Mismatches**: The tests use specific topic names (`test.events`, `test.worker.events`). Ensure that any changes to the production topic naming convention do not inadvertently break these tests.
*   **Schema Evolution**: Because these tests use real JSON serialization, they are sensitive to breaking changes in the domain models. Always run this suite when modifying fields in `TaskDescriptor` or `WorkerCapabilities`.
*   **Resource Cleanup**: The tests use try-with-resources blocks for producers and consumers. If you extend these tests, ensure that any new Kafka clients are properly closed to prevent socket leaks in the test environment.
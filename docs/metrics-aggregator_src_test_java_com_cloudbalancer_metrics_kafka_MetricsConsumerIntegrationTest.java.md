# File: metrics-aggregator/src/test/java/com/cloudbalancer/metrics/kafka/MetricsConsumerIntegrationTest.java

## Overview

`MetricsConsumerIntegrationTest` is a critical integration test suite for the `metrics-aggregator` service. It validates the end-to-end Kafka message consumption pipeline, ensuring that various system events—such as worker metrics, heartbeats, and task completion events—are correctly ingested, processed, and persisted to the underlying data stores.

**Note:** This file is a **HOTSPOT**. It is in the top 25% for both change frequency and complexity. As a primary integration point for Kafka-based event processing, any changes to the event schema or consumer logic require rigorous verification here to prevent regressions in data persistence.

## Public API

The class provides the following test methods to verify the Kafka consumer pipeline:

*   **`workerMetricsStored()`**: Verifies that `WorkerMetricsEvent` messages are correctly deserialized and stored in the `WorkerMetricsRepository`.
*   **`heartbeatStored()`**: Validates that `WorkerHeartbeatEvent` messages update the `WorkerHeartbeatRepository` with the correct health state.
*   **`taskCompletedEventStored()`**: Ensures `TaskCompletedEvent` messages are correctly mapped to `TaskMetricsRecord` entries.
*   **`multipleMetricsSnapshotsStored()`**: Tests the consumer's ability to handle high-volume or sequential metrics updates for a single worker.
*   **`unknownEventTypeIgnored()`**: Confirms that the consumer gracefully ignores unsupported event types (e.g., `TaskSubmittedEvent`), preventing invalid data from polluting the metrics database.

## Dependencies

*   **Spring Boot Test**: Provides the `@SpringBootTest` context and dependency injection for repositories.
*   **Testcontainers (Kafka)**: Manages an ephemeral Kafka broker instance for isolated integration testing.
*   **Repositories**:
    *   `WorkerMetricsRepository`
    *   `WorkerHeartbeatRepository`
    *   `TaskMetricsRepository`
*   **Kafka Clients**: Used to produce test events to the Kafka topics (`workers.metrics`, `workers.heartbeat`, `tasks.events`).
*   **Awaitility**: Used to handle asynchronous assertions, as Kafka message processing is non-blocking.

## Usage Notes

### Testing Lifecycle
*   **Setup**: The `setUp()` method initializes a `KafkaProducer` pointing to the ephemeral `KafkaContainer` bootstrap server.
*   **Teardown**: The `tearDown()` method ensures database state isolation by clearing all repositories after each test case and closing the producer.

### Asynchronous Assertions
Because Kafka consumption is asynchronous, tests utilize `Awaitility` to poll the database until the expected record count is reached. 
*   **Timeout**: Tests are configured with a 15-second timeout. If your environment is resource-constrained, ensure the `await()` duration is sufficient to avoid flaky tests.

### Example: Adding a New Event Type
To add support for a new event type, follow this pattern:
1.  **Produce**: Use `producer.send()` with a `ProducerRecord` containing the serialized event.
2.  **Await**: Use `await().until(() -> repository.count() >= 1)` to wait for the consumer to process the message.
3.  **Verify**: Perform assertions on the repository record using AssertJ to ensure data integrity.

### Potential Pitfalls
*   **Schema Mismatch**: Since events are serialized via `JsonUtil`, ensure that any changes to the event POJOs are backward compatible or updated in both the producer and consumer.
*   **Topic Configuration**: Ensure the topics used in the test match the `@KafkaListener` configurations in the main application code.
*   **Hotspot Risk**: Given this is a high-activity file, always run this suite before committing changes to any `*Listener` or `*Repository` classes to ensure the event pipeline remains intact.
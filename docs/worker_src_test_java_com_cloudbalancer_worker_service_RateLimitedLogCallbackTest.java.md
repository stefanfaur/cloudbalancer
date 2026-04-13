# File: worker/src/test/java/com/cloudbalancer/worker/service/RateLimitedLogCallbackTest.java

## Overview

`RateLimitedLogCallbackTest` is a JUnit 5 test suite designed to verify the functionality of the `RateLimitedLogCallback` service. This component is responsible for throttling log output sent to Kafka to prevent system overload while ensuring that all logs are eventually delivered via a flushing mechanism.

The tests ensure that the callback correctly distinguishes between immediate log transmission and buffered log storage based on defined time intervals.

## Public API

The test class validates the following behaviors of `RateLimitedLogCallback`:

*   **`batchesLinesWithinInterval`**: Verifies that the first log line is sent immediately, subsequent lines within the defined rate-limiting interval are buffered, and a manual `flush()` triggers the transmission of buffered content.
*   **`flushSendsAllBufferedLines`**: Confirms that multiple buffered log lines are transmitted to the `KafkaTemplate` upon calling `flush()`, regardless of the configured interval.
*   **`emptyFlushDoesNothing`**: Ensures that calling `flush()` on an instance with no buffered logs does not result in unnecessary interactions with the `KafkaTemplate`.

## Dependencies

*   **JUnit 5 (Jupiter)**: Used for test lifecycle management and assertions.
*   **Mockito**: Used for mocking the `KafkaTemplate` to verify interactions without requiring a live Kafka broker.
*   **Spring Kafka**: The test validates interactions with `KafkaTemplate<String, String>`.
*   **Java Time API**: Used to simulate timestamps for log events.

## Usage Notes

*   **Mocking**: The `KafkaTemplate` is mocked using `@Mock` and `@ExtendWith(MockitoExtension.class)`. Any changes to the `RateLimitedLogCallback` constructor or its interaction with Kafka will require updates to these test mocks.
*   **Test Isolation**: Each test method generates a unique `UUID` for the `taskId` to ensure that log streams are isolated and do not interfere with one another during verification.
*   **Execution**: These tests are intended to run as part of the standard Maven/Gradle build lifecycle for the `worker` module. They do not require an external environment or database.
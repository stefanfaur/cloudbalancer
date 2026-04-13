# File: common/src/test/java/com/cloudbalancer/common/event/EventSerializationTest.java

## Overview

`EventSerializationTest` is a critical JUnit 5 test suite responsible for validating the JSON serialization and deserialization integrity of the `CloudBalancerEvent` class hierarchy. 

**Note:** This file is a **HOTSPOT** (top 25% for both change frequency and complexity). It serves as the primary safeguard against breaking changes in the event-driven communication layer of the system. Any modifications to event models or their Jackson annotations should be thoroughly verified against this test suite to prevent runtime deserialization failures in production.

The suite ensures that:
1. Polymorphic deserialization works correctly using the `eventType` discriminator.
2. All event fields are correctly mapped during round-trip conversions.
3. JSON payloads contain the necessary metadata required by downstream consumers.

## Public API

The class provides a series of test methods that act as integration tests for the `JsonUtil` mapper configuration:

* `taskSubmittedEventRoundTrip()`: Validates `TaskSubmittedEvent` serialization.
* `taskStateChangedEventRoundTrip()`: Validates `TaskStateChangedEvent` state transitions.
* `taskCompletedEventRoundTrip()`: Validates `TaskCompletedEvent` payload integrity.
* `workerRegisteredEventRoundTrip()`: Validates `WorkerRegisteredEvent` and nested `WorkerCapabilities`.
* `workerHeartbeatEventRoundTrip()`: Validates `WorkerHeartbeatEvent` health state reporting.
* `scalingEventRoundTrip()`: Validates `ScalingEvent` decision logging.
* `drainCommandRoundTrip()`: Validates `DrainCommand` serialization.
* `eventTypeDiscriminatorPresent()`: Ensures the `eventType` field is explicitly present in the JSON output, which is required for polymorphic type resolution.

## Dependencies

* `com.cloudbalancer.common.model.*`: Contains the domain models and event definitions being tested.
* `com.cloudbalancer.common.util.JsonUtil`: Provides the configured `ObjectMapper` instance used across the system.
* `com.fasterxml.jackson.databind.ObjectMapper`: The core library used for JSON processing.
* `org.junit.jupiter.api.Test`: JUnit 5 testing framework.
* `org.assertj.core.api.Assertions`: Fluent assertion library for readable test verification.

## Usage Notes

### Adding New Event Types
When introducing a new event type to the `CloudBalancerEvent` hierarchy, you **must** add a corresponding test case to this file. Failure to do so may result in:
1. **Deserialization Errors**: If the new class is not registered in the `@JsonSubTypes` annotation of the base `CloudBalancerEvent` class.
2. **Missing Discriminators**: If the `eventType` field is not correctly populated, the system will fail to identify the event type during consumption.

### Debugging Deserialization Failures
If a test fails during `mapper.readValue(...)`:
1. **Check Annotations**: Ensure the new event class is correctly annotated with `@JsonTypeName` matching the `eventType` string.
2. **Check Constructor**: Ensure the class has a properly annotated constructor (e.g., `@JsonCreator`) or that the fields are accessible to Jackson (e.g., using `jackson-module-parameter-names`).
3. **Verify Discriminator**: Use the `eventTypeDiscriminatorPresent` test as a template to verify that your JSON output contains the required `eventType` key.

### Example: Adding a Test Case
To test a new event, follow the round-trip pattern:

```java
@Test
void myNewEventRoundTrip() throws Exception {
    var event = new MyNewEvent("id", Instant.now(), "payload");
    
    // 1. Serialize
    String json = mapper.writeValueAsString(event);
    
    // 2. Verify discriminator
    assertThat(json).contains("\"eventType\":\"MY_NEW_EVENT\"");
    
    // 3. Deserialize and verify
    CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
    assertThat(deserialized).isInstanceOf(MyNewEvent.class);
    assertThat(((MyNewEvent) deserialized).payload()).isEqualTo("payload");
}
```
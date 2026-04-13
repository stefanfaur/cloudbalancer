# File: common/src/main/java/com/cloudbalancer/common/util/JsonUtil.java

## Overview

`JsonUtil` is a utility class providing a centralized, pre-configured `ObjectMapper` instance for JSON serialization and deserialization across the `cloudbalancer` project. By encapsulating the `ObjectMapper` configuration, it ensures consistency in how Java objects—particularly those involving date-time types—are converted to and from JSON format.

## Public API

### `JsonUtil` (Class)
A `final` utility class that cannot be instantiated.

### `mapper()` (Method)
Returns the shared, static `ObjectMapper` instance.

*   **Signature**: `public static ObjectMapper mapper()`
*   **Returns**: A configured `ObjectMapper` instance.

## Dependencies

*   **Jackson Databind**: `com.fasterxml.jackson.databind.ObjectMapper`
*   **Jackson Databind Serialization**: `com.fasterxml.jackson.databind.SerializationFeature`
*   **Jackson JSR310 Module**: `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`

## Usage Notes

The `ObjectMapper` provided by `JsonUtil` is configured with the following defaults:
1.  **JavaTimeModule**: Registered to support Java 8 Date/Time API (e.g., `LocalDateTime`, `ZonedDateTime`).
2.  **Date Serialization**: `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` is disabled, ensuring that dates are serialized as ISO-8601 strings rather than numeric timestamps.

**Example Usage:**

```java
import com.cloudbalancer.common.util.JsonUtil;

// Serialize an object to JSON
String json = JsonUtil.mapper().writeValueAsString(myObject);

// Deserialize JSON to an object
MyObject obj = JsonUtil.mapper().readValue(json, MyObject.class);
```

This utility is commonly used in JPA `AttributeConverter` implementations and DTO processing throughout the application to maintain uniform JSON formatting.
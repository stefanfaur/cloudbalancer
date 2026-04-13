# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/WeightsConverter.java

## Overview

The `WeightsConverter` class is a JPA `AttributeConverter` responsible for mapping a `Map<String, Integer>` object to a JSON-formatted `String` for database storage and vice versa. This utility facilitates the persistence of dynamic weight configurations—often used in scheduling strategies like `CustomStrategy`—within relational database columns.

## Public API

### `WeightsConverter`
A class annotated with `@Converter` that implements `AttributeConverter<Map<String, Integer>, String>`.

### `convertToDatabaseColumn(Map<String, Integer> attribute)`
Serializes a `Map` of weights into a JSON string. If the input map is null, it defaults to an empty map.
*   **Throws**: `IllegalArgumentException` if JSON serialization fails.

### `convertToEntityAttribute(String dbData)`
Deserializes a JSON string retrieved from the database back into a `HashMap<String, Integer>`.
*   **Throws**: `IllegalArgumentException` if JSON deserialization fails.

## Dependencies

*   **`com.cloudbalancer.common.util.JsonUtil`**: Used for accessing the shared Jackson `ObjectMapper` instance.
*   **`com.fasterxml.jackson.core`**: Provides core JSON processing capabilities.
*   **`jakarta.persistence`**: Provides the JPA `AttributeConverter` interface and `@Converter` annotation.
*   **`java.util`**: Provides `HashMap` and `Map` collections.

## Usage Notes

*   **JPA Integration**: This converter is designed to be used with the `@Convert` annotation on entity fields that store weight configurations.
*   **Error Handling**: The converter wraps checked `JsonProcessingException` exceptions in `IllegalArgumentException`. Ensure that the data stored in the database column is valid JSON to avoid runtime errors during entity loading.
*   **Null Safety**: The `convertToDatabaseColumn` method handles null inputs by converting them to an empty JSON object (`{}`), ensuring the database column remains consistent.
*   **Mutability**: The `convertToEntityAttribute` method returns a new `HashMap`, ensuring that the resulting map is mutable and safe for modification within the application logic.
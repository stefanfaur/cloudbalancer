# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/WorkerCapabilitiesConverter.java

## Overview

The `WorkerCapabilitiesConverter` is a JPA `AttributeConverter` responsible for mapping the `WorkerCapabilities` domain object to a JSON-formatted `String` for storage in the database, and vice-versa. This allows complex capability objects to be persisted as simple text columns within database entities.

## Public API

### `WorkerCapabilitiesConverter`
Implements `AttributeConverter<WorkerCapabilities, String>`. Annotated with `@Converter` to register it with the JPA provider.

### `convertToDatabaseColumn(WorkerCapabilities attribute)`
Serializes the `WorkerCapabilities` object into a JSON string.
- **Parameters**: `attribute` - The `WorkerCapabilities` instance to serialize.
- **Returns**: A JSON string representation of the capabilities.
- **Throws**: `IllegalArgumentException` if serialization fails.

### `convertToEntityAttribute(String dbData)`
Deserializes a JSON string retrieved from the database back into a `WorkerCapabilities` object.
- **Parameters**: `dbData` - The JSON string stored in the database.
- **Returns**: The reconstructed `WorkerCapabilities` instance.
- **Throws**: `IllegalArgumentException` if deserialization fails.

## Dependencies

- `com.cloudbalancer.common.model.WorkerCapabilities`: The domain model being converted.
- `com.cloudbalancer.common.util.JsonUtil`: Utility class providing the Jackson `ObjectMapper` instance.
- `com.fasterxml.jackson.core.JsonProcessingException`: Exception handling for JSON operations.
- `jakarta.persistence.AttributeConverter`: JPA interface for custom type conversion.
- `jakarta.persistence.Converter`: JPA annotation for marking the class as a converter.

## Usage Notes

- This converter is intended for use with JPA entities that contain a `WorkerCapabilities` field.
- It relies on `JsonUtil` for consistent JSON serialization/deserialization across the application.
- Ensure that the database column mapped to the `WorkerCapabilities` field is of a text-based type (e.g., `VARCHAR`, `TEXT`, or `JSONB`) capable of holding the serialized JSON string.
- If serialization or deserialization fails, the converter throws an `IllegalArgumentException`, which will propagate up to the JPA persistence layer.
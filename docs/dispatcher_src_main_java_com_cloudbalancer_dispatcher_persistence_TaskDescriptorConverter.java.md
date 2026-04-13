# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/TaskDescriptorConverter.java

## Overview

The `TaskDescriptorConverter` is a JPA `AttributeConverter` designed to facilitate the persistence of `TaskDescriptor` objects within the database. It handles the bidirectional transformation between the `TaskDescriptor` domain model and its JSON string representation. This allows complex task metadata to be stored as a single column in a relational database while maintaining type safety within the application layer.

## Public API

### `TaskDescriptorConverter` (Class)
Implements `AttributeConverter<TaskDescriptor, String>`. This class is annotated with `@Converter`, allowing it to be automatically registered by the JPA provider when applied to entity fields.

### `convertToDatabaseColumn(TaskDescriptor attribute)`
Converts the `TaskDescriptor` object into a JSON `String` for storage in the database.
*   **Parameters**: `attribute` - The `TaskDescriptor` instance to serialize.
*   **Returns**: A JSON string representation of the descriptor.
*   **Throws**: `IllegalArgumentException` if serialization fails.

### `convertToEntityAttribute(String dbData)`
Converts the JSON `String` retrieved from the database back into a `TaskDescriptor` object.
*   **Parameters**: `dbData` - The JSON string stored in the database.
*   **Returns**: The deserialized `TaskDescriptor` instance.
*   **Throws**: `IllegalArgumentException` if deserialization fails.

## Dependencies

*   `com.cloudbalancer.common.model.TaskDescriptor`: The domain model being persisted.
*   `com.cloudbalancer.common.util.JsonUtil`: Utility class providing the Jackson `ObjectMapper` instance for serialization/deserialization.
*   `com.fasterxml.jackson.core.JsonProcessingException`: Exception handling for JSON transformation errors.
*   `jakarta.persistence.AttributeConverter`: JPA interface for custom type mapping.
*   `jakarta.persistence.Converter`: JPA annotation to mark the class as a converter.

## Usage Notes

*   **JPA Integration**: To use this converter, annotate the relevant field in your JPA Entity with `@Convert(converter = TaskDescriptorConverter.class)`.
*   **Error Handling**: The converter wraps checked `JsonProcessingException` instances into unchecked `IllegalArgumentException`. Ensure that the data stored in the database column is valid JSON, or the application will throw an exception during entity loading.
*   **Serialization**: This class relies on `JsonUtil.mapper()`. Ensure that the `TaskDescriptor` class is compatible with the Jackson configuration provided by `JsonUtil` (e.g., proper getters/setters or default constructors).
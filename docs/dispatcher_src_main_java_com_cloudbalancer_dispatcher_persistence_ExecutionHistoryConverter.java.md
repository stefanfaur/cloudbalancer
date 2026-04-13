# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/persistence/ExecutionHistoryConverter.java

## Overview

The `ExecutionHistoryConverter` is a JPA `AttributeConverter` designed to manage the persistence of `List<ExecutionAttempt>` objects. It facilitates the bidirectional transformation between a Java `List` of execution attempts and a JSON-formatted `String` stored in the database. This allows complex execution history data to be stored as a single column in a relational database while maintaining type safety within the application layer.

## Public API

### `ExecutionHistoryConverter`
A class implementing `AttributeConverter<List<ExecutionAttempt>, String>`. It is annotated with `@Converter` to register it with the JPA provider.

### `convertToDatabaseColumn(List<ExecutionAttempt> attribute)`
Serializes a list of `ExecutionAttempt` objects into a JSON string. If the input list is `null`, it defaults to an empty list before serialization.
*   **Throws**: `IllegalArgumentException` if JSON serialization fails.

### `convertToEntityAttribute(String dbData)`
Deserializes a JSON string retrieved from the database back into a `List<ExecutionAttempt>`. The result is wrapped in a new `ArrayList` to ensure mutability.
*   **Throws**: `IllegalArgumentException` if JSON deserialization fails.

## Dependencies

*   **`com.cloudbalancer.common.model.ExecutionAttempt`**: The domain model representing an individual execution attempt.
*   **`com.cloudbalancer.common.util.JsonUtil`**: Utility class providing access to the configured Jackson `ObjectMapper`.
*   **`com.fasterxml.jackson`**: Core library used for JSON processing.
*   **`jakarta.persistence`**: JPA API for defining the converter interface and registration.

## Usage Notes

*   **Null Handling**: The `convertToDatabaseColumn` method safely handles `null` inputs by converting them to an empty list, ensuring the database column contains a valid JSON array (`[]`) rather than a null value.
*   **Error Handling**: Both conversion methods wrap `JsonProcessingException` in an `IllegalArgumentException`. This is intended to signal critical configuration or data integrity issues during the persistence lifecycle.
*   **Integration**: This converter is intended for use in JPA entities using the `@Convert` annotation on fields of type `List<ExecutionAttempt>`.
*   **Performance**: Since this relies on JSON serialization/deserialization for every read/write operation, it is best suited for fields where the execution history list size is moderate.
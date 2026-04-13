# File: common/src/main/java/com/cloudbalancer/common/executor/ValidationResult.java

## Overview

The `ValidationResult` class is a lightweight, immutable data carrier designed to encapsulate the outcome of validation operations within the `com.cloudbalancer.common.executor` package. Implemented as a Java `record`, it provides a standardized way to communicate whether a validation process succeeded and, if not, to provide a list of descriptive error messages.

## Public API

### `ok()`
Creates a successful `ValidationResult`.
- **Returns**: A `ValidationResult` instance where `valid` is `true` and the error list is empty.

### `invalid(String... errors)`
Creates a failed `ValidationResult` containing one or more error messages.
- **Parameters**: `errors` — A variable argument list of `String` objects representing the validation failures.
- **Returns**: A `ValidationResult` instance where `valid` is `false` and the error list contains the provided messages.

## Dependencies

- `java.util.List`: Used to store and return the collection of error messages associated with the validation result.

## Usage Notes

- **Immutability**: As a Java `record`, the `ValidationResult` is immutable. Once created, the status and error list cannot be modified.
- **Pattern Matching**: This class is intended to be used as a return type for validation logic, allowing callers to easily check the `valid()` boolean flag or iterate through the `errors()` list to handle failures.
- **Example**:
  ```java
  ValidationResult result = validator.validate(input);
  if (!result.valid()) {
      result.errors().forEach(System.err::println);
  }
  ```
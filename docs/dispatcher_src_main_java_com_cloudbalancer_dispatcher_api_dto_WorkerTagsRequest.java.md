# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/WorkerTagsRequest.java

## Overview

The `WorkerTagsRequest` class is a simple Data Transfer Object (DTO) used within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a container for transmitting a collection of worker tags, typically used when updating or filtering worker nodes based on specific metadata or labels within the cloud balancer system.

## Public API

### `WorkerTagsRequest`

```java
public record WorkerTagsRequest(Set<String> tags) {}
```

#### Constructors
*   `WorkerTagsRequest(Set<String> tags)`: Initializes a new request object with the provided set of tags.

#### Methods
*   `tags()`: Returns the `Set<String>` containing the worker tags associated with this request.

## Dependencies

*   `java.util.Set`: Used to store the unique collection of tag strings.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the set of tags cannot be modified.
*   **Data Handling**: The `tags` field expects a `Set`, ensuring that duplicate tags are automatically handled by the collection implementation.
*   **Integration**: This DTO is intended for use in API request bodies where a client needs to communicate a list of tags to the dispatcher service. Ensure that the JSON deserializer used by the application (e.g., Jackson) is configured to handle the `record` type correctly.
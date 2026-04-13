# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/CreateAgentTokenResponse.java

## Overview

The `CreateAgentTokenResponse` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as the standardized response structure returned to clients after a successful request to generate an authentication token for a cloud agent.

## Public API

### Constructor
`public CreateAgentTokenResponse(UUID id, String token, String label)`

Creates a new instance of the response object.

### Components
*   **`id` (UUID)**: The unique identifier assigned to the newly created agent token.
*   **`token` (String)**: The actual authentication token string used by the agent to authenticate with the dispatcher.
*   **`label` (String)**: A human-readable identifier or alias associated with the agent token for administrative purposes.

## Dependencies

*   `java.util.UUID`: Used for the unique identification of the token record.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the values for `id`, `token`, and `label` cannot be modified.
*   **Serialization**: This DTO is intended to be serialized to JSON when returned by the dispatcher's REST API endpoints. Ensure that the consuming client is configured to handle the `UUID` type correctly.
*   **Security**: The `token` field contains sensitive credential information. Ensure that logging configurations and API response interceptors are set up to mask or exclude this field from logs to prevent security leaks.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/AgentTokenSummary.java

## Overview

The `AgentTokenSummary` class is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It provides a concise, immutable representation of an agent's authentication token metadata. This object is primarily used to convey the state and history of tokens managed by the cloud balancer dispatcher.

## Public API

### Constructor
`public AgentTokenSummary(UUID id, String label, String createdBy, Instant createdAt, Instant lastUsedAt, boolean revoked)`

### Components
*   **`id` (UUID)**: The unique identifier for the agent token.
*   **`label` (String)**: A human-readable identifier or alias assigned to the token.
*   **`createdBy` (String)**: The identifier of the user or system entity that generated the token.
*   **`createdAt` (Instant)**: The timestamp indicating when the token was initially created.
*   **`lastUsedAt` (Instant)**: The timestamp indicating the most recent time the token was utilized for authentication.
*   **`revoked` (boolean)**: A flag indicating whether the token has been explicitly invalidated or revoked.

## Dependencies

*   `java.time.Instant`: Used for precise timestamp tracking of token lifecycle events.
*   `java.util.UUID`: Used for the unique identification of the token entity.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the state of the `AgentTokenSummary` cannot be modified.
*   **Serialization**: Being a standard DTO, it is intended for use in API responses, typically serialized to JSON format by frameworks like Jackson.
*   **Data Integrity**: Ensure that `createdAt` and `lastUsedAt` are populated with valid UTC timestamps to maintain consistency across the distributed system.
*   **Maintainer**: Primary maintenance is handled by **sfaur**.
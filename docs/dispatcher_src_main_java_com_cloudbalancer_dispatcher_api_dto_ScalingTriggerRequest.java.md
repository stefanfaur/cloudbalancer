# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/api/dto/ScalingTriggerRequest.java

## Overview

The `ScalingTriggerRequest` is a Java `record` used as a Data Transfer Object (DTO) within the `com.cloudbalancer.dispatcher.api.dto` package. It serves as a structured container for requests to trigger scaling operations within the cloud balancer system.

## Public API

### `ScalingTriggerRequest`

A record representing the parameters required to initiate a scaling action.

**Constructor:**
```java
public ScalingTriggerRequest(String action, int count, String agentId)
```

**Components:**
*   `action` (String): The type of scaling action to be performed (e.g., "scale-up", "scale-down").
*   `count` (int): The number of instances or units to be affected by the scaling action.
*   `agentId` (String): The unique identifier of the agent or node associated with the scaling request.

## Dependencies

This class is a standard Java `record` and does not depend on any external libraries or internal project classes beyond the standard Java Development Kit (JDK).

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Once instantiated, the values for `action`, `count`, and `agentId` cannot be modified.
*   **JSON Serialization**: This DTO is intended to be used with JSON serialization frameworks (such as Jackson). Ensure that the framework is configured to support Java records if using older versions of library dependencies.
*   **Validation**: This class does not perform internal validation. It is recommended to validate the `action` string and `count` integer values at the service layer or via JSR-303/JSR-380 annotations if integrated with a validation framework.
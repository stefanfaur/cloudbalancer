# File: common/src/main/java/com/cloudbalancer/common/model/SecurityLevel.java

## Overview

The `SecurityLevel` enum defines the standardized security tiers used across the CloudBalancer architecture. It categorizes execution environments or entities based on their trust status, facilitating consistent security policy enforcement throughout the system.

## Public API

### `SecurityLevel` (Enum)

The enum provides the following constants:

*   **`TRUSTED`**: Represents entities or environments with full access and high-level trust within the system.
*   **`SANDBOXED`**: Represents environments with restricted access, typically used for executing untrusted or third-party code with limited system visibility.
*   **`ISOLATED`**: Represents the most restrictive tier, where entities are completely partitioned from the core system to prevent lateral movement or unauthorized data access.

## Dependencies

This file has no external dependencies and does not import any other classes or packages.

## Usage Notes

*   **Type Safety**: This enum is intended to be used as a type-safe identifier for security policies. It is frequently referenced in authentication and authorization logic to determine the appropriate permissions for a given context.
*   **Cross-Platform Mapping**: When working with the `web-dashboard` (TypeScript), ensure that the backend `SecurityLevel` values are mapped correctly to the corresponding frontend authorization types (e.g., `Role`) to maintain consistent security posture across the full stack.
*   **Extensibility**: While currently limited to three levels, this enum can be extended to include more granular security tiers as the system's complexity grows.
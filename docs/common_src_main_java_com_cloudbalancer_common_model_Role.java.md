# File: common/src/main/java/com/cloudbalancer/common/model/Role.java

## Overview

The `Role` enumeration defines the standardized access levels and authorization tiers used across the CloudBalancer ecosystem. It serves as the single source of truth for user permissions, ensuring consistent role-based access control (RBAC) definitions between the backend services and the frontend dashboard.

## Public API

### `enum Role`

The following constants represent the available security roles within the system:

| Constant | Description |
| :--- | :--- |
| `ADMIN` | Full administrative access, including system configuration and user management. |
| `OPERATOR` | Operational access for managing resources and monitoring system health. |
| `VIEWER` | Read-only access for monitoring and reporting purposes. |
| `API_CLIENT` | Restricted access intended for programmatic service-to-service authentication. |

## Dependencies

This class is a standalone enumeration and does not depend on any external libraries or internal project modules.

## Usage Notes

*   **Type Safety**: Use this enum to enforce type safety when implementing security filters, method-level authorization (e.g., `@PreAuthorize`), or database mappings.
*   **Cross-Platform Sync**: This enum is mirrored in the `web-dashboard` via the `Role` TypeScript type alias. When adding or modifying roles, ensure that both the Java `Role` enum and the corresponding TypeScript definition are updated to maintain synchronization.
*   **Serialization**: When using this enum in JSON payloads (e.g., via Jackson), it is serialized as a string matching the constant name. Ensure that any custom deserializers or database converters handle these string representations correctly.
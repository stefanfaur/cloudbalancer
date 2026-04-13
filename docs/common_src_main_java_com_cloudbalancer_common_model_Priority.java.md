# File: common/src/main/java/com/cloudbalancer/common/model/Priority.java

## Overview

The `Priority` enum defines the standard urgency levels used across the CloudBalancer system to categorize tasks, alerts, and worker processes. It provides a type-safe mechanism for enforcing priority constraints throughout the application, ensuring consistent handling of resource allocation and scheduling logic.

## Public API

The `Priority` enum exposes the following constants:

| Constant | Description |
| :--- | :--- |
| `CRITICAL` | Highest urgency; tasks require immediate processing and resource allocation. |
| `HIGH` | Elevated urgency; tasks should be prioritized over standard operations. |
| `NORMAL` | Standard urgency; default level for routine tasks. |
| `LOW` | Lowest urgency; tasks are processed when resources are available. |

## Dependencies

This enum is a standalone component and does not depend on any external libraries or internal modules.

## Usage Notes

- **Cross-Module Consistency**: This enum is used by the `dispatcher` module (e.g., in `BulkReprioritizeRequest`) to manage task scheduling and by the `web-dashboard` (via the TypeScript `Priority` type alias) to maintain UI consistency.
- **Type Safety**: When implementing new scheduling strategies or API endpoints, use this enum instead of raw strings or integers to ensure compile-time validation and prevent invalid priority assignments.
- **Extensibility**: While currently limited to four levels, any changes to these constants should be coordinated with the frontend dashboard to ensure the UI remains synchronized with the backend logic.
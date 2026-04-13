# File: common/src/main/java/com/cloudbalancer/common/model/ExecutorType.java

## Overview

`ExecutorType` is a core enumeration within the `com.cloudbalancer.common.model` package. It defines the set of supported execution environments available within the CloudBalancer ecosystem. This enum serves as a centralized source of truth for identifying the runtime context or technology stack required to execute a specific task or workload.

## Public API

The `ExecutorType` enum provides the following constants:

| Constant | Description |
| :--- | :--- |
| `SHELL` | Executes tasks via native system shell commands. |
| `SPRING_BATCH` | Executes tasks using the Spring Batch framework. |
| `DOCKER` | Executes tasks within isolated Docker containers. |
| `WASM` | Executes tasks using WebAssembly runtimes. |
| `SIMULATED` | Executes tasks in a non-production, simulated environment for testing. |
| `PYTHON` | Executes tasks using a Python runtime environment. |
| `AI_INFERENCE` | Executes tasks specifically optimized for AI/ML model inference. |

## Dependencies

This enum is a standalone type and does not depend on any external libraries or internal project classes. It is frequently referenced by other components in the `common` module, such as `WorkerCapabilities`, to define the operational scope of worker nodes.

## Usage Notes

- **Strict Typing**: Use `ExecutorType` to enforce type safety when configuring task definitions or worker capabilities.
- **Extensibility**: When adding support for new execution runtimes, ensure the corresponding constant is added to this enum to maintain consistency across the system.
- **Cross-Platform Compatibility**: This enum is mirrored in the frontend via `web-dashboard/src/api/types.ts`. Ensure that any changes to the Java enum are reflected in the TypeScript definition to prevent serialization or contract mismatches.
- **Integration**: Components responsible for task scheduling or worker assignment should use this enum to filter or match tasks to appropriate worker nodes based on their supported `ExecutorType`.
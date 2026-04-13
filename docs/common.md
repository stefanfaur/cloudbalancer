# Module: common

## Overview

The `common` module serves as the foundational library for the CloudBalancer ecosystem. It provides the core domain models, event definitions, execution abstractions, and utility classes required by both the control plane and worker agents. 

This module establishes the "language" of the system, defining how tasks are represented, how workers communicate their health and capabilities, and how different execution environments (Docker, Python, Shell) are abstracted under a unified interface. It is designed for high reliability, utilizing immutable Java records and strict state machine transitions to ensure system consistency.

## Public API Summary

The module's public API is organized into four primary functional domains:

### 1. Task Execution & Orchestration
*   **`TaskExecutor`**: The primary interface for task execution. Implementations include `DockerExecutor`, `PythonExecutor`, `ShellExecutor`, and `SimulatedExecutor`.
*   **`TaskEnvelope`**: The core state machine container for task lifecycles, managing transitions between states like `SUBMITTED`, `QUEUED`, `RUNNING`, and terminal states.
*   **`TaskContext` & `ExecutionHandle`**: Contextual wrappers for managing task-specific metadata, working directories, and log callbacks.

### 2. Event-Driven Messaging
*   **`CloudBalancerEvent`**: A sealed interface hierarchy representing all system events (e.g., `TaskSubmittedEvent`, `WorkerHeartbeatEvent`, `ScalingEvent`).
*   **`AgentEvent` & `AgentCommand`**: Polymorphic interfaces for agent-to-dispatcher communication, supporting robust JSON serialization via Jackson.

### 3. Domain Models
*   **Worker Management**: `WorkerInfo`, `WorkerCapabilities`, `WorkerHealthState`, and `WorkerMetrics` provide a comprehensive schema for monitoring and managing cluster nodes.
*   **Configuration**: `ScalingPolicy`, `ExecutionPolicy`, `ResourceProfile`, and `TaskConstraints` provide type-safe, immutable configurations for cluster behavior.

### 4. Utilities
*   **`JsonUtil`**: A centralized utility providing a pre-configured `ObjectMapper` to ensure consistent polymorphic serialization across the entire project.
*   **`NodeRuntime`**: An abstraction layer for local worker lifecycle management, with `LocalThreadRuntime` as the primary implementation for testing and local execution.

## Architecture Notes

*   **Immutability**: The module heavily utilizes Java `record` types to ensure thread safety and data integrity across asynchronous event streams.
*   **Polymorphic Serialization**: The system relies on Jackson-based polymorphic deserialization for events and commands. Developers adding new event types must ensure they are correctly registered within the sealed interface hierarchies to maintain serialization compatibility.
*   **Hotspots**: 
    *   `PythonExecutor.java` and `DockerExecutor.java` are identified as system hotspots due to their complexity in managing external process lifecycles and resource isolation.
    *   The `TaskEnvelope` state machine is critical; any modifications to state transitions must be verified against `TaskEnvelopeStateMachineTest` to prevent illegal state transitions.
*   **Testing Strategy**: The module employs a rigorous testing strategy, including:
    *   **Round-trip Serialization Tests**: Ensuring all DTOs and events can survive JSON serialization cycles.
    *   **Integration Testing**: Using `KafkaTestContainer` to validate end-to-end event propagation.
    *   **State Machine Validation**: Exhaustive testing of task lifecycle transitions to ensure business rule compliance.
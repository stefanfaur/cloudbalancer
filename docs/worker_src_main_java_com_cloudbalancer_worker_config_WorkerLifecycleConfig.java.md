# File: worker/src/main/java/com/cloudbalancer/worker/config/WorkerLifecycleConfig.java

## Overview

`WorkerLifecycleConfig` is a Spring `@Configuration` class responsible for defining lifecycle-related beans within the `worker` service. It provides a centralized mechanism to track the operational state of the worker node, specifically regarding its "draining" status.

## Public API

### `WorkerLifecycleConfig`
The configuration class that registers lifecycle management beans into the Spring application context.

### `drainingFlag()`
*   **Return Type**: `AtomicBoolean`
*   **Description**: Provides a thread-safe boolean flag used to indicate whether the worker node is currently in a "draining" state. When set to `true`, this flag signals that the worker should stop accepting new tasks and finish processing existing ones before shutting down.

## Dependencies

*   `org.springframework.context.annotation.Bean`: Used to register the `AtomicBoolean` as a managed bean.
*   `org.springframework.context.annotation.Configuration`: Marks the class as a source of bean definitions for the Spring IoC container.
*   `java.util.concurrent.atomic.AtomicBoolean`: Provides the thread-safe primitive used for state management.

## Usage Notes

*   **State Management**: The `drainingFlag` bean is intended to be injected into components responsible for task processing (e.g., `WorkerCommandListener`) and lifecycle management.
*   **Thread Safety**: Because the bean is an `AtomicBoolean`, it is safe to read and update the draining status across multiple threads without explicit synchronization.
*   **Default State**: The flag is initialized to `false` upon application startup, indicating the worker is in a normal operating mode.
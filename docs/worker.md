# Module: worker

## Overview

The `worker` module is a core component of the CloudBalancer infrastructure, responsible for executing distributed tasks. It functions as a Spring Boot-based worker node that consumes task assignments via Apache Kafka, manages task lifecycles, handles artifact staging, and reports system metrics and heartbeat status.

The module is designed for high reliability and observability, featuring integrated circuit breakers for fault tolerance, configurable execution environments (Docker, Python, Shell), and built-in chaos engineering capabilities to simulate network and processing delays.

## Public API Summary

The module exposes several key services and configuration components:

*   **Task Orchestration**: `TaskExecutionService` serves as the primary engine for task lifecycle management, including execution, artifact collection, and result publishing.
*   **Kafka Integration**: `TaskAssignmentListener` and `WorkerCommandListener` handle incoming work and administrative control signals, respectively.
*   **Artifact Management**: `ArtifactService` provides utilities for staging input files and collecting output artifacts from task execution environments.
*   **Monitoring & Health**: `MetricsReporter` handles periodic system telemetry and heartbeat signals, while `RateLimitedLogCallback` ensures efficient log streaming to the control plane.
*   **Configuration**: `WorkerProperties` provides a centralized schema for configuring worker resources (CPU, memory), supported executors, and environment-specific settings (Docker/Python/Shell).
*   **Fault Tolerance**: `CircuitBreakerConfiguration` defines resilience policies, and `WorkerChaosService` provides hooks for testing system robustness.

## Architecture Notes

*   **Event-Driven Design**: The worker operates primarily as a consumer of Kafka topics. It is designed to be stateless regarding task state, relying on the central dispatcher for assignments and reporting results back through the message bus.
*   **Modular Execution**: The architecture uses a factory pattern in `ExecutorConfig` to dynamically instantiate task executors based on the `WorkerProperties` configuration. This allows the worker to support multiple execution modes (e.g., local shell, Docker containers, or Python environments) interchangeably.
*   **Resilience**: The system incorporates Resilience4j for circuit breaking, particularly around result production, to prevent cascading failures when the control plane or message broker is under stress.
*   **Lifecycle Management**: The `WorkerLifecycleConfig` maintains the operational state of the node (e.g., a "draining" flag), allowing for graceful shutdowns and maintenance without losing inflight tasks.
*   **Observability**: The module implements a robust telemetry pipeline, with dedicated services for metrics reporting and rate-limited log streaming, ensuring that the control plane maintains visibility into worker health without overwhelming the network.
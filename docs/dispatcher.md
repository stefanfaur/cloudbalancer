# Module: dispatcher

## Overview

The `dispatcher` module is the central control plane for the CloudBalancer infrastructure. It is a Spring Boot-based service responsible for orchestrating task scheduling, managing worker node lifecycles, and providing real-time monitoring capabilities. The module acts as the "brain" of the system, bridging the gap between incoming task requests and the distributed compute agents that execute them.

Key responsibilities include:
*   **Task Orchestration**: Managing the lifecycle of tasks from submission and queuing to assignment, execution, and final result processing.
*   **Worker Management**: Tracking worker health, resource availability, and capabilities, with support for both agent-based and Docker-based runtimes.
*   **Auto-Scaling**: Dynamically adjusting the worker pool size based on real-time metrics (CPU utilization, queue pressure) to optimize resource usage.
*   **Security & Auth**: Enforcing role-based access control (RBAC) via JWTs, managing agent registration tokens, and implementing rate limiting.
*   **Observability**: Providing real-time updates to dashboards and log streams via WebSockets and Kafka event integration.

## Public API Summary

The module exposes a comprehensive REST API for system management:

*   **Task Management**: Endpoints for submitting, listing, and performing bulk operations (cancel, retry, reprioritize) on tasks.
*   **Worker & Agent Management**: APIs to list workers, update worker tags, and manually trigger worker termination.
*   **Auto-Scaling Control**: Interfaces to monitor scaling status, update scaling policies, and trigger manual scaling events.
*   **Authentication**: Endpoints for user login, logout, and token refresh.
*   **Agent Registration**: Secure endpoints for agents to register themselves and retrieve Kafka credentials.
*   **Chaos Engineering**: Administrative endpoints to inject latency or simulate failures for resilience testing.

## Architecture Notes

The `dispatcher` module follows a layered architecture designed for scalability and fault tolerance:

*   **Scheduling Pipeline**: A pluggable, multi-stage pipeline (`SchedulingPipeline`) that filters and scores potential workers for a task. It supports various strategies (Round Robin, Least Connections, Resource Fit) managed by a `SchedulingStrategyFactory`.
*   **Persistence Layer**: Built on Spring Data JPA, using PostgreSQL as the primary data store. It utilizes custom JPA `AttributeConverter` implementations (e.g., `TaskDescriptorConverter`, `WorkerCapabilitiesConverter`) to handle complex JSONB data structures efficiently.
*   **Event-Driven Core**: Heavily reliant on Apache Kafka for asynchronous communication. Listeners (e.g., `AgentEventListener`, `TaskResultListener`, `WorkerMetricsListener`) handle real-time updates from the distributed worker fleet.
*   **Security Infrastructure**: Implements a robust security model using Spring Security, featuring JWT-based authentication, custom `RateLimitFilter` (using `bucket4j`), and `JwtHandshakeInterceptor` for secure WebSocket connections.
*   **Runtime Abstraction**: Uses a `NodeRuntime` abstraction to decouple the dispatcher from specific infrastructure providers, supporting both generic agents and direct Docker container management (`DockerRuntime`).
*   **Resilience**: Incorporates `Resilience4j` circuit breakers for external service interactions and a `RetryScanner` background service to handle task recovery and dead-lettering.
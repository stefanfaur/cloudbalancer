# File: worker-agent/src/main/java/com/cloudbalancer/agent/AgentApplication.java

## Overview

`AgentApplication` serves as the primary entry point for the `worker-agent` service. It is a Spring Boot application configured to manage the lifecycle of the agent, including the initialization of background tasks and scheduled processes required for cloud balancing operations.

## Public API

### `AgentApplication` (Class)
The main application class annotated with `@SpringBootApplication` and `@EnableScheduling`.

*   **`main(String[] args)`**: The static entry point of the application. It bootstraps the Spring context using `SpringApplication.run()`, initiating the component scanning and configuration loading for the agent.

## Dependencies

*   **Spring Boot**: Utilizes `org.springframework.boot.SpringApplication` and `org.springframework.boot.autoconfigure.SpringBootApplication` for framework bootstrapping.
*   **Spring Scheduling**: Utilizes `org.springframework.scheduling.annotation.EnableScheduling` to enable support for `@Scheduled` tasks within the agent.

## Usage Notes

*   **Execution**: This class should be executed as the main class of the `worker-agent` JAR file.
*   **Scheduling**: Because `@EnableScheduling` is present, any beans within the application context that utilize the `@Scheduled` annotation will be automatically detected and executed according to their defined cron expressions or fixed delays.
*   **Configuration**: The application automatically scans for `@Configuration` classes (such as `AgentConfig`) within the `com.cloudbalancer.agent` package and its sub-packages to assemble the application context.
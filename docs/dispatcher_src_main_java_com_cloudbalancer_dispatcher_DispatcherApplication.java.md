# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/DispatcherApplication.java

## Overview

`DispatcherApplication` is the primary entry point for the `dispatcher` service within the CloudBalancer infrastructure. It is a Spring Boot application configured to manage the lifecycle of the dispatcher node, which is responsible for orchestrating task distribution and managing resource allocation across the system.

The application is annotated with `@SpringBootApplication` to enable auto-configuration and component scanning, and `@EnableScheduling` to support background task execution required for periodic resource monitoring and ledger synchronization.

## Public API

### `DispatcherApplication` (Class)
The main configuration class that bootstraps the Spring application context.

### `main(String[] args)` (Method)
The static entry point for the Java Virtual Machine.
- **Parameters**: `args` (Command-line arguments passed to the application).
- **Functionality**: Invokes `SpringApplication.run()` to initialize the Spring context and start the embedded server.

## Dependencies

- `org.springframework.boot:spring-boot`: Provides the core framework for the application lifecycle.
- `org.springframework.boot:spring-boot-autoconfigure`: Enables automatic configuration of Spring beans based on classpath dependencies.
- `org.springframework:spring-context`: Provides the scheduling capabilities enabled via `@EnableScheduling`.

## Usage Notes

- **Startup**: The application is designed to be run as a standalone JAR. Ensure that all necessary environment variables for database connectivity and service discovery are configured before execution.
- **Scheduling**: Because `@EnableScheduling` is active, ensure that any `@Scheduled` tasks defined in other components are configured with appropriate fixed rates or cron expressions to avoid overloading the dispatcher.
- **Initialization**: This class works in conjunction with `ResourceLedgerInitializer` to ensure that the system state is consistent upon startup. Avoid heavy blocking operations in the `main` thread to ensure a timely startup sequence.
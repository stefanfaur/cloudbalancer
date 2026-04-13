# File: worker/src/main/java/com/cloudbalancer/worker/WorkerApplication.java

## Overview

`WorkerApplication` is the primary entry point for the `worker` service within the CloudBalancer infrastructure. It is a Spring Boot application designed to initialize the worker node, manage its lifecycle, and enable background task execution.

The class is annotated with `@SpringBootApplication`, which triggers component scanning and auto-configuration, and `@EnableScheduling`, which allows the application to perform periodic background tasks (such as heartbeats or health reporting) using Spring's scheduling capabilities.

## Public API

### `WorkerApplication` (class)
The main configuration class for the worker service.

### `main` (method)
```java
public static void main(String[] args)
```
The standard entry point for the Java application. It invokes `SpringApplication.run` to bootstrap the Spring context and start the worker service.

## Dependencies

- `org.springframework.boot.SpringApplication`: Used to launch the Spring Boot application.
- `org.springframework.boot.autoconfigure.SpringBootApplication`: Provides the core configuration and auto-configuration features.
- `org.springframework.scheduling.annotation.EnableScheduling`: Enables support for `@Scheduled` tasks within the application context.

## Usage Notes

- **Startup**: Running the `main` method will initialize the Spring context, which triggers the `WorkerRegistrationService` to register the node with the central CloudBalancer controller.
- **Scheduling**: Because `@EnableScheduling` is present, any beans within the application context annotated with `@Scheduled` will be automatically executed according to their defined intervals.
- **Environment**: Ensure that the necessary environment variables or configuration properties (e.g., controller URLs, worker credentials) are provided to the Spring environment upon startup for successful registration and operation.
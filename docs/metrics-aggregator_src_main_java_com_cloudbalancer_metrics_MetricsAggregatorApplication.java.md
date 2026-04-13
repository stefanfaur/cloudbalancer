# File: metrics-aggregator/src/main/java/com/cloudbalancer/metrics/MetricsAggregatorApplication.java

## Overview

`MetricsAggregatorApplication` is the primary entry point for the `metrics-aggregator` service within the CloudBalancer infrastructure. It is a standard Spring Boot application class responsible for bootstrapping the application context, enabling auto-configuration, and initiating the component scanning process for the metrics aggregation module.

## Public API

### `MetricsAggregatorApplication` (Class)
The main configuration class annotated with `@SpringBootApplication`. This annotation enables:
*   **@EnableAutoConfiguration**: Automatically configures beans based on the classpath.
*   **@ComponentScan**: Scans the current package and sub-packages for Spring-managed components.
*   **@Configuration**: Allows for additional bean definitions within this class.

### `main(String[] args)` (Method)
The static entry point of the application.
*   **Parameters**: `args` (String array) - Command-line arguments passed during application startup.
*   **Functionality**: Invokes `SpringApplication.run()` to launch the Spring context and start the embedded server.

## Dependencies

*   **org.springframework.boot:spring-boot**: Provides the core framework for the standalone application.
*   **org.springframework.boot:spring-boot-autoconfigure**: Enables the automatic configuration of the Spring application context.

## Usage Notes

*   **Execution**: This class should be executed to start the `metrics-aggregator` service. It can be run via an IDE, the `java -jar` command after building the project, or through build tools like Maven or Gradle (e.g., `mvn spring-boot:run`).
*   **Configuration**: As the root configuration class, any additional global beans or custom configurations required for the metrics aggregation service should be defined here or in classes scanned by the default component scan.
*   **Testing**: Integration tests for this application context are located in `MetricsAggregatorApplicationTest.java`, which ensures that the application context loads correctly without errors.
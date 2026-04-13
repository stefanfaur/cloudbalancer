# Infrastructure: metrics-aggregator/Dockerfile

## Purpose
The `metrics-aggregator/Dockerfile` defines the container image specification for the `metrics-aggregator` service. It is designed to package a pre-compiled Java application into a lightweight, production-ready container image based on the Eclipse Temurin JRE 21 Alpine distribution.

## Key Targets/Stages
This Dockerfile uses a single-stage build process:
*   **Runtime Stage**: Utilizes `eclipse-temurin:21-jre-alpine` as the base image to provide a minimal Java 21 runtime environment, optimized for security and small footprint.

## Configuration
*   **Base Image**: `eclipse-temurin:21-jre-alpine` (Java 21 JRE).
*   **Working Directory**: `/app`.
*   **Artifact Source**: Expects the compiled JAR file to be located at `metrics-aggregator/build/libs/*.jar` relative to the build context root.
*   **Exposed Ports**: `8081` (The default port for the metrics-aggregator service).
*   **Entrypoint**: Executes the application using `java -jar app.jar`.

## Operational Notes
*   **Build Context**: This Dockerfile must be built from the root of the repository to ensure the `COPY` command correctly resolves the path to the build artifacts.
*   **Dependency Management**: This file assumes that the JAR file has been built by a CI/CD pipeline or a local build task prior to the `docker build` command. Ensure the build process is completed before triggering the image build.
*   **Alpine Considerations**: The use of `alpine` provides a significantly smaller image size but uses `musl libc` instead of `glibc`. Ensure that any native libraries or dependencies included in the JAR are compatible with Alpine Linux.
*   **Memory Limits**: When deploying to Kubernetes or other orchestrators, ensure that memory limits are configured, as the JVM may require specific flags (e.g., `-XX:MaxRAMPercentage`) to respect container memory constraints.
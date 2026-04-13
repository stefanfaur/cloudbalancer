# Infrastructure: dispatcher/Dockerfile

## Purpose

The `dispatcher/Dockerfile` defines the container image specification for the Dispatcher microservice. It utilizes a lightweight Java runtime environment to package and execute the compiled Spring Boot (or similar JVM-based) application. This image is optimized for production deployment within container orchestration platforms like Kubernetes.

## Key Targets/Stages

This Dockerfile uses a single-stage build process:

*   **Runtime Stage**: Based on `eclipse-temurin:21-jre-alpine`. This stage assumes that the application artifact (`.jar`) has already been built by a CI/CD pipeline or a previous build step, focusing strictly on the execution environment to keep the image size minimal.

## Configuration

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Base Image** | `eclipse-temurin:21-jre-alpine` | Uses the Alpine Linux distribution with OpenJDK 21 JRE for a small footprint and improved security. |
| **Working Directory** | `/app` | Sets the base directory for the application files. |
| **Artifact Source** | `dispatcher/build/libs/*.jar` | Expects the build artifact to be located in the local `dispatcher/build/libs/` directory. |
| **Exposed Port** | `8080` | The standard port on which the Dispatcher service listens for incoming traffic. |
| **Entrypoint** | `java -jar app.jar` | Executes the application using the Java runtime. |

## Operational Notes

*   **Build Prerequisites**: This Dockerfile expects the JAR file to be pre-built. Ensure your CI pipeline (e.g., GitHub Actions, Jenkins, or GitLab CI) executes `./gradlew :dispatcher:build` or equivalent before running `docker build`.
*   **Alpine Considerations**: The use of `alpine` ensures a minimal attack surface but uses `musl libc` instead of `glibc`. If your application relies on native libraries (JNI) that require `glibc`, you may need to switch to the `eclipse-temurin:21-jre` (Debian-based) image.
*   **Memory Management**: When running in Kubernetes, ensure that resource limits (`resources.limits.memory`) are defined. Since Java 21 is container-aware, the JVM will respect these limits, but it is recommended to set `JAVA_OPTS` or `container-memory` flags to prevent OOM (Out of Memory) kills.
*   **Security**: The application currently runs as the `root` user by default in this Dockerfile. For production environments with strict security requirements, it is recommended to add a non-privileged user and switch to it using `USER appuser` before the `ENTRYPOINT`.
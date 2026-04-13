# Infrastructure: worker/Dockerfile

## Purpose

The `worker/Dockerfile` defines the container image for the background worker service. It provides a lightweight, Java-based runtime environment capable of executing JVM-based applications while maintaining the ability to interact with the host's Docker daemon and execute Python scripts. This setup is typically used for task processing, CI/CD job execution, or infrastructure automation tasks that require both Java and shell-level container orchestration.

## Key Targets/Stages

This Dockerfile uses a single-stage build process:

*   **Runtime Stage**: Based on `eclipse-temurin:21-jre-alpine`. It installs necessary system dependencies and configures the entry point to execute the packaged Java application.

## Configuration

*   **Base Image**: `eclipse-temurin:21-jre-alpine` (Java 21 JRE on Alpine Linux).
*   **System Dependencies**:
    *   `python3`: Installed via `apk` to support Python-based task execution or helper scripts.
    *   `docker-cli`: Installed via `apk` to allow the worker to interact with the Docker daemon (e.g., for "Docker-in-Docker" or sidecar management).
*   **Working Directory**: `/app`
*   **Artifact Deployment**: Copies the compiled JAR file from `worker/build/libs/*.jar` into the container as `app.jar`.
*   **Entrypoint**: Executes the application using `java -jar app.jar`.

## Operational Notes

*   **Docker Socket Access**: To utilize the `docker-cli` installed in this image, the container must be run with the Docker socket mounted (e.g., `-v /var/run/docker.sock:/var/run/docker.sock`). Ensure the container user has the appropriate permissions to access the socket.
*   **Alpine Limitations**: This image uses `musl libc` instead of `glibc`. If the Java application relies on native libraries (JNI) compiled specifically for `glibc`, you may encounter `java.lang.UnsatisfiedLinkError`.
*   **Build Context**: This Dockerfile expects to be built from the project root directory, as it references `worker/build/libs/*.jar`. Ensure the build process (e.g., Gradle/Maven) has completed before triggering the Docker build.
*   **Security**: The container runs as the `root` user by default. For production environments, consider adding a non-privileged user and group to the Dockerfile and switching to that user before the `ENTRYPOINT`.
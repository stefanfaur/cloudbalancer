# Infrastructure: worker-agent/Dockerfile

## Purpose

The `worker-agent/Dockerfile` defines the container image for the application's worker-agent service. This service is responsible for executing background tasks, which requires both a Java runtime environment and the ability to interact with the host's Docker daemon to manage containerized workloads.

## Key Targets/Stages

This Dockerfile uses a single-stage build process:

*   **Runtime Environment**: Based on `eclipse-temurin:21-jre-alpine`. This provides a lightweight, production-ready Java 21 runtime environment optimized for Alpine Linux.

## Configuration

*   **Base Image**: `eclipse-temurin:21-jre-alpine` (Java 21 JRE).
*   **Dependencies**: Installs `docker-cli` via `apk` to enable the agent to communicate with the Docker daemon.
*   **Working Directory**: `/app`.
*   **Artifact Source**: Expects the application JAR file to be pre-built and located at `worker-agent/build/libs/*.jar` on the host machine.
*   **Entrypoint**: Executes the application using `java -jar app.jar`.

## Operational Notes

*   **Docker Socket Access**: For the `docker-cli` to function correctly, the container must be run with the Docker socket mounted as a volume (e.g., `-v /var/run/docker.sock:/var/run/docker.sock`). Ensure the user running the container has the appropriate permissions to access the socket.
*   **Build Context**: This Dockerfile assumes the build context is the root of the repository. When building, ensure the command is executed from the project root:
    ```bash
    docker build -f worker-agent/Dockerfile .
    ```
*   **Alpine Limitations**: Since this image uses Alpine Linux, it utilizes `musl libc` instead of `glibc`. While standard Java applications are generally compatible, ensure any native dependencies or JNI libraries are compatible with Alpine.
*   **Security**: The container runs as the `root` user by default. For production environments, it is recommended to create a non-privileged user within the Dockerfile and switch to it using the `USER` instruction.
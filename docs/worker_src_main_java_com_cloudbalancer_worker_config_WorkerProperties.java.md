# File: worker/src/main/java/com/cloudbalancer/worker/config/WorkerProperties.java

## Overview

`WorkerProperties` is the central configuration class for the CloudBalancer worker node. It leverages Spring Boot’s `@ConfigurationProperties` to bind external configuration values (typically from `application.yml`) to the application context. 

This class defines the operational parameters of the worker, including resource allocation, supported execution environments (Shell, Docker, Python), security constraints, and communication intervals for metrics and heartbeats.

## Public API

### Core Properties
- `getId()` / `setId(String)`: Unique identifier for the worker node.
- `getSupportedExecutors()` / `setSupportedExecutors(Set<ExecutorType>)`: Defines which execution engines are enabled.
- `getCpuCores()` / `setCpuCores(int)`: Configures CPU resource limits.
- `getMemoryMb()` / `setMemoryMb(int)`: Configures memory resource limits in MB.
- `getDiskMb()` / `setDiskMb(int)`: Configures disk storage limits in MB.
- `getTags()` / `setTags(Set<String>)`: Metadata tags for worker categorization.
- `getMetricsIntervalMs()` / `setMetricsIntervalMs(long)`: Frequency of metrics reporting.
- `getHeartbeatIntervalMs()` / `setHeartbeatIntervalMs(long)`: Frequency of heartbeat signals to the controller.

### Nested Configuration Classes
- **`ShellConfig`**: Manages command-line execution security.
    - `getBlockedCommands()`: Returns a set of forbidden shell commands.
    - `getMaxOutputBytes()`: Limits the size of command output buffers.
- **`DockerConfig`**: Manages container runtime settings.
    - `getHost()`: Defines the Docker daemon socket or TCP endpoint.
- **`PythonConfig`**: Manages Python execution environment.
    - `getPythonBinary()`: Path/name of the Python executable.
    - `isNetworkIsolation()`: Toggles network namespace isolation for Python tasks.
- **`ArtifactConfig`**: Manages artifact management settings.
    - `getMaxSizeBytes()`: Maximum size for artifact uploads/downloads.
    - `getDispatcherUrl()`: The endpoint for the artifact dispatcher service.

## Dependencies

- `com.cloudbalancer.common.model.ExecutorType`: Used to define supported execution modes.
- `org.springframework.boot.context.properties.ConfigurationProperties`: Enables automatic binding from YAML/Properties files.
- `org.springframework.stereotype.Component`: Marks the class for Spring component scanning.
- `java.util.Set`: Used for collection-based configuration properties.

## Usage Notes

### Configuration Binding
The properties are bound under the `cloudbalancer.worker` prefix in your configuration files. 

**Example `application.yml` configuration:**
```yaml
cloudbalancer:
  worker:
    id: "production-worker-01"
    cpu-cores: 8
    memory-mb: 16384
    shell:
      max-output-bytes: 2097152
    python:
      network-isolation: true
```

### Security Considerations
- **Shell Commands**: The `ShellConfig` includes a default list of dangerous commands (e.g., `rm -rf /`, `shutdown`). When overriding `blockedCommands`, ensure that the new set includes these or equivalent security restrictions to prevent malicious code execution.
- **Network Isolation**: The `PythonConfig.networkIsolation` flag is enabled by default. Disabling this may expose the host network to tasks executed via the Python engine.

### Resource Management
- The `cpuCores`, `memoryMb`, and `diskMb` values are used by the worker to report capacity to the dispatcher. Ensure these values accurately reflect the underlying host hardware to prevent over-subscription or scheduling failures.
- The `metricsIntervalMs` and `heartbeatIntervalMs` should be tuned based on network latency and the desired granularity of the monitoring dashboard. Setting these too low may increase network overhead on the dispatcher.
# File: dispatcher/src/main/java/com/cloudbalancer/dispatcher/config/ScalingProperties.java

## Overview

The `ScalingProperties` class is a Spring-managed configuration component that encapsulates the settings for the cloud dispatcher's auto-scaling mechanism. It is bound to the `cloudbalancer.dispatcher.scaling` configuration prefix, allowing parameters to be injected via `application.properties` or `application.yml` files.

This class manages three primary categories of configuration:
1.  **Scaling Logic**: Thresholds and intervals for CPU-based and queue-based scaling decisions.
2.  **Runtime Environment**: Docker-specific settings, including image names, network configurations, and Kafka connectivity.
3.  **Default Resource Allocation**: Standard resource profiles (CPU, memory, disk) for newly provisioned workers.

## Public API

The class provides standard getters and setters for the following properties:

### Scaling Logic
- `runtimeMode`: The execution environment (default: `DOCKER`).
- `enabled`: Toggle for the auto-scaling service.
- `evaluationIntervalMs`: Frequency of scaling checks in milliseconds.
- `cpuHighThreshold` / `cpuLowThreshold`: Percentage thresholds for triggering scale-up or scale-down events.
- `reactiveWindowSeconds`: Time window for reactive scaling analysis.
- `scaleDownWindowSeconds`: Cooldown period before scaling down.
- `queuePressureWindowSeconds`: Time window for monitoring queue depth.
- `queuePressureRatioThreshold`: Ratio threshold for queue pressure scaling.

### Docker Configuration
- `dockerWorkerImage`: The container image used for workers.
- `dockerNetworkName`: The Docker network to attach workers to.
- `kafkaBootstrapInternal`: Internal Kafka broker address for worker communication.
- `drainTimeSeconds`: Grace period for shutting down workers.

### Default Resource Allocation
- `defaultWorkerExecutorTypes`: Comma-separated list of supported executor types.
- `defaultWorkerCpuCores`: Default CPU core allocation.
- `defaultWorkerMemoryMb`: Default memory allocation in MB.
- `defaultWorkerDiskMb`: Default disk allocation in MB.

## Dependencies

- `org.springframework.boot.context.properties.ConfigurationProperties`: Used to map external configuration properties to the class fields.
- `org.springframework.stereotype.Component`: Marks the class as a Spring-managed bean for dependency injection.

## Usage Notes

- **Configuration Binding**: To override default values, define properties in your configuration file using the `cloudbalancer.dispatcher.scaling` prefix. For example:
  ```yaml
  cloudbalancer:
    dispatcher:
      scaling:
        enabled: true
        cpu-high-threshold: 85.0
        default-worker-memory-mb: 16384
  ```
- **Integration**: This class is primarily consumed by the `AutoScalerService`, which uses these properties to perform periodic evaluation of the cluster state and trigger scaling actions.
- **Runtime Mode**: While the default is `DOCKER`, ensure that the environment is correctly configured with the specified `dockerNetworkName` and `kafkaBootstrapInternal` if using the default mode.
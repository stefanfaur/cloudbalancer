# File: common/src/main/java/com/cloudbalancer/common/model/WorkerCapabilities.java

## Overview

`WorkerCapabilities` is an immutable data record that encapsulates the operational constraints and identity of a worker node within the CloudBalancer ecosystem. It defines the specific execution environments a worker can handle, the total compute resources available, and a set of metadata tags used for scheduling and filtering.

## Public API

### `supportsExecutor(ExecutorType type)`

Determines whether the worker node is capable of running a specific type of task executor.

*   **Parameters**: 
    *   `type` (`ExecutorType`): The executor environment to verify.
*   **Returns**: `boolean` - `true` if the executor type is present in the `supportedExecutors` set, otherwise `false`.

## Dependencies

*   `java.util.Set`: Used to manage collections of supported executors and metadata tags.
*   `com.cloudbalancer.common.model.ExecutorType`: Defines the supported execution environments.
*   `com.cloudbalancer.common.model.ResourceProfile`: Represents the hardware/compute capacity of the worker.

## Usage Notes

*   **Immutability**: As a Java `record`, this class is immutable. Any changes to a worker's capabilities require the instantiation of a new `WorkerCapabilities` object.
*   **Scheduling**: The `supportsExecutor` method is intended to be used by the scheduler or load balancer to filter eligible workers before assigning tasks.
*   **Integration**: This class is utilized by the `worker-agent` to report its current state to the control plane and by the `ExecutorConfig` to validate runtime configurations.
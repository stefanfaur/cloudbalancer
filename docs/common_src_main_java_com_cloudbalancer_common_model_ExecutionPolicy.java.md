# File: common/src/main/java/com/cloudbalancer/common/model/ExecutionPolicy.java

## Overview

`ExecutionPolicy.java` defines the configuration model for task execution behavior within the CloudBalancer system. Implemented as an immutable Java `record`, it encapsulates the parameters governing retry logic, timeouts, and error handling strategies for distributed tasks.

## Public API

### `ExecutionPolicy` (Record)
The primary data structure containing the following fields:
* `int maxRetries`: The maximum number of attempts allowed for a task.
* `int timeoutSeconds`: The duration in seconds before a task is considered timed out.
* `BackoffStrategy retryBackoffStrategy`: The strategy used to calculate delays between retries.
* `FailureAction failureAction`: The action to perform when the maximum retry limit is reached.

### `defaults()`
```java
public static ExecutionPolicy defaults()
```
Returns a pre-configured `ExecutionPolicy` instance with the following standard settings:
* **Max Retries**: 3
* **Timeout**: 300 seconds
* **Backoff Strategy**: `BackoffStrategy.EXPONENTIAL_WITH_JITTER`
* **Failure Action**: `FailureAction.RETRY`

## Dependencies

* `com.cloudbalancer.common.model.BackoffStrategy` (Enum)
* `com.cloudbalancer.common.model.FailureAction` (Enum)

## Usage Notes

* **Immutability**: As a Java `record`, `ExecutionPolicy` is immutable. Any modifications to the policy require creating a new instance.
* **Standard Configuration**: It is recommended to use the `defaults()` method as a base for custom policies to ensure consistency across the system.
* **Integration**: This policy is typically utilized by the task execution engine to determine the lifecycle management of a `TaskDescriptor`.
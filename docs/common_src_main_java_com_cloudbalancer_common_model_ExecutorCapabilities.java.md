# File: common/src/main/java/com/cloudbalancer/common/model/ExecutorCapabilities.java

## Overview

`ExecutorCapabilities` is a Java `record` used to define the operational requirements and constraints of an execution environment within the CloudBalancer system. It encapsulates the hardware, networking, and security prerequisites necessary for a task or executor to function correctly.

## Public API

### Constructor
`public ExecutorCapabilities(boolean requiresDocker, boolean requiresNetworkAccess, ResourceProfile maxResourceCeiling, SecurityLevel securityLevel)`

*   **requiresDocker**: Indicates if the executor must run within a Docker container environment.
*   **requiresNetworkAccess**: Indicates if the executor requires external network connectivity.
*   **maxResourceCeiling**: A `ResourceProfile` object defining the upper limits of CPU, memory, and storage available to the executor.
*   **securityLevel**: A `SecurityLevel` enum value defining the isolation or permission requirements for the execution.

## Dependencies

*   `com.cloudbalancer.common.model.ResourceProfile`: Used to define the resource constraints.
*   `com.cloudbalancer.common.model.SecurityLevel`: Used to define the security context.

## Usage Notes

*   As a Java `record`, this class is immutable and provides built-in implementations for `equals()`, `hashCode()`, and `toString()`.
*   This model is primarily used by the scheduler to match incoming tasks with available infrastructure nodes that meet the specified capabilities.
*   Ensure that `maxResourceCeiling` is correctly instantiated before passing it to the constructor, as null values may cause `NullPointerException` during runtime validation in downstream services.
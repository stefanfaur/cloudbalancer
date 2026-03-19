package com.cloudbalancer.common.model;

import java.util.Set;

public record WorkerCapabilities(
    Set<ExecutorType> supportedExecutors,
    ResourceProfile totalResources,
    Set<String> tags
) {
    public boolean supportsExecutor(ExecutorType type) {
        return supportedExecutors.contains(type);
    }
}

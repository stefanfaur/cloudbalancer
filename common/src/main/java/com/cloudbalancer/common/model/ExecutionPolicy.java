package com.cloudbalancer.common.model;

public record ExecutionPolicy(
    int maxRetries,
    int timeoutSeconds,
    BackoffStrategy retryBackoffStrategy,
    FailureAction failureAction
) {
    public static ExecutionPolicy defaults() {
        return new ExecutionPolicy(3, 300, BackoffStrategy.EXPONENTIAL_WITH_JITTER, FailureAction.RETRY);
    }
}

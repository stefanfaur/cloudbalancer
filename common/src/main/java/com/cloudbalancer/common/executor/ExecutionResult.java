package com.cloudbalancer.common.executor;

public record ExecutionResult(int exitCode, String stdout, String stderr, long durationMs, boolean timedOut) {
    public boolean succeeded() { return exitCode == 0 && !timedOut; }
}

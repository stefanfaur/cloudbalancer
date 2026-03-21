package com.cloudbalancer.common.executor;

import java.time.Instant;

@FunctionalInterface
public interface LogCallback {
    void onLogLine(String line, boolean isStderr, Instant timestamp);
}

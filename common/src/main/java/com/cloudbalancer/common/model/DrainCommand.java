package com.cloudbalancer.common.model;

import java.time.Instant;

public record DrainCommand(
    String workerId,
    int drainTimeSeconds,
    Instant timestamp
) implements WorkerCommand {
    @Override
    public String commandType() { return "DRAIN"; }
}

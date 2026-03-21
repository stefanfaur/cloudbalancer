package com.cloudbalancer.common.executor;

import java.time.Instant;
import java.util.UUID;

public record LogMessage(UUID taskId, String line, boolean stderr, Instant timestamp) {}

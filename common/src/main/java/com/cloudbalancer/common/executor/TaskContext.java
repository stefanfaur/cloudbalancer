package com.cloudbalancer.common.executor;

import java.nio.file.Path;
import java.util.UUID;

public record TaskContext(UUID taskId, Path workingDirectory) {}

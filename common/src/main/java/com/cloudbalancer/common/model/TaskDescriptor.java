package com.cloudbalancer.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskDescriptor(
    ExecutorType executorType,
    Map<String, Object> executionSpec,
    ResourceProfile resourceProfile,
    TaskConstraints constraints,
    Priority priority,
    ExecutionPolicy executionPolicy,
    TaskIO io
) {}

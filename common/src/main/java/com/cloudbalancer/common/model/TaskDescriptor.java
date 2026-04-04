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
) {
    /**
     * Returns a copy with null optional fields replaced by sensible defaults.
     * ResourceProfile and TaskIO remain nullable (legitimately optional).
     */
    public TaskDescriptor withDefaults() {
        return new TaskDescriptor(
            executorType,
            executionSpec,
            resourceProfile,
            constraints != null ? constraints : TaskConstraints.unconstrained(),
            priority != null ? priority : Priority.NORMAL,
            executionPolicy != null ? executionPolicy : ExecutionPolicy.defaults(),
            io
        );
    }
}

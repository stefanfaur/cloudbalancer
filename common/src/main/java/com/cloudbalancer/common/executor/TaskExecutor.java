package com.cloudbalancer.common.executor;

import com.cloudbalancer.common.model.ExecutorCapabilities;
import com.cloudbalancer.common.model.ExecutorType;
import java.util.Map;

public interface TaskExecutor {
    ExecutionResult execute(Map<String, Object> spec, ResourceAllocation allocation, TaskContext context);
    ValidationResult validate(Map<String, Object> spec);
    ResourceEstimate estimateResources(Map<String, Object> spec);
    ExecutorCapabilities getCapabilities();
    ExecutorType getExecutorType();
    void cancel(ExecutionHandle handle);
}

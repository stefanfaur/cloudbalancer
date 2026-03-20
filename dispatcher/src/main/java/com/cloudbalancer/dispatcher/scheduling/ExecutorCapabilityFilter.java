package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;

public class ExecutorCapabilityFilter implements WorkerFilter {

    @Override
    public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates) {
        return candidates.stream()
            .filter(w -> w.getCapabilities().supportsExecutor(task.getExecutorType()))
            .toList();
    }
}

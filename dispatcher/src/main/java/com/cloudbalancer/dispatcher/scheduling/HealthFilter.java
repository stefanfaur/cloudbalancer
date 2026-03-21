package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;

public class HealthFilter implements WorkerFilter {

    @Override
    public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates) {
        return candidates.stream()
            .filter(w -> w.getHealthState() == WorkerHealthState.HEALTHY
                      || w.getHealthState() == WorkerHealthState.RECOVERING)
            .toList();
    }
}

package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.TaskConstraints;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;

public class ConstraintFilter implements WorkerFilter {

    @Override
    public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates) {
        TaskConstraints constraints = task.getDescriptor().constraints();
        return candidates.stream()
            .filter(w -> matchesConstraints(w, constraints))
            .toList();
    }

    private boolean matchesConstraints(WorkerRecord worker, TaskConstraints constraints) {
        // Required tags: worker must have all
        if (constraints.requiredTags() != null && !constraints.requiredTags().isEmpty()) {
            if (!worker.getCapabilities().tags().containsAll(constraints.requiredTags())) {
                return false;
            }
        }
        // Blacklist: worker must not be in it
        if (constraints.blacklistedWorkers() != null && !constraints.blacklistedWorkers().isEmpty()) {
            if (constraints.blacklistedWorkers().contains(worker.getId())) {
                return false;
            }
        }
        // Whitelist: if non-empty, worker must be in it
        if (constraints.whitelistedWorkers() != null && !constraints.whitelistedWorkers().isEmpty()) {
            if (!constraints.whitelistedWorkers().contains(worker.getId())) {
                return false;
            }
        }
        return true;
    }
}

package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;

public class ResourceSufficiencyFilter implements WorkerFilter {

    @Override
    public List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates) {
        ResourceProfile required = task.getDescriptor().resourceProfile();
        if (required == null) {
            return candidates;
        }
        return candidates.stream()
            .filter(w -> hasAvailableResources(w, required))
            .toList();
    }

    private boolean hasAvailableResources(WorkerRecord worker, ResourceProfile required) {
        var total = worker.getCapabilities().totalResources();
        if (total == null) {
            return false;
        }
        int freeCpu = total.cpuCores() - worker.getAllocatedCpu();
        int freeMemory = total.memoryMB() - worker.getAllocatedMemoryMb();
        int freeDisk = total.diskMB() - worker.getAllocatedDiskMb();
        return freeCpu >= required.cpuCores()
            && freeMemory >= required.memoryMB()
            && freeDisk >= required.diskMB();
    }
}

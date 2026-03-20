package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

public class ResourceAvailabilityScorer implements WorkerScorer {

    @Override
    public int score(TaskRecord task, WorkerRecord worker) {
        ResourceProfile total = worker.getCapabilities().totalResources();
        if (total.cpuCores() == 0 && total.memoryMB() == 0 && total.diskMB() == 0) {
            return 0;
        }

        double cpuFree = total.cpuCores() > 0
            ? (double) (total.cpuCores() - worker.getAllocatedCpu()) / total.cpuCores()
            : 1.0;
        double memFree = total.memoryMB() > 0
            ? (double) (total.memoryMB() - worker.getAllocatedMemoryMb()) / total.memoryMB()
            : 1.0;
        double diskFree = total.diskMB() > 0
            ? (double) (total.diskMB() - worker.getAllocatedDiskMb()) / total.diskMB()
            : 1.0;

        // Average of free resource percentages, scaled to 0-100
        double avgFree = (cpuFree + memFree + diskFree) / 3.0;
        return (int) Math.round(Math.max(0, Math.min(100, avgFree * 100)));
    }

    @Override
    public String getScorerName() {
        return "resourceAvailability";
    }
}

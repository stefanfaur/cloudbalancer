package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.common.model.WorkerHealthState;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.time.Duration;
import java.time.Instant;

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
        int baseScore = (int) Math.round(Math.max(0, Math.min(100, avgFree * 100)));

        // Apply recovery penalty if RECOVERING
        if (worker.getHealthState() == WorkerHealthState.RECOVERING && worker.getRecoveryStartedAt() != null) {
            long recoverySeconds = Duration.between(worker.getRecoveryStartedAt(), Instant.now()).getSeconds();
            float penalty;
            if (recoverySeconds >= 40) {
                penalty = 0.75f;  // Near-normal at 40-60s
            } else if (recoverySeconds >= 20) {
                penalty = 0.5f;   // Moderate at 20-40s
            } else {
                penalty = 0.25f;  // Severe penalty first 20s
            }
            return (int) (baseScore * penalty);
        }

        return baseScore;
    }

    @Override
    public String getScorerName() {
        return "resourceAvailability";
    }
}

package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Shared helpers for filter/scorer unit tests — no Spring context needed.
 */
final class FilterTestHelper {

    private FilterTestHelper() {}

    static TaskRecord taskWithExecutor(ExecutorType type) {
        return taskWith(type, 1, 256, 50, TaskConstraints.unconstrained(), Priority.NORMAL);
    }

    static TaskRecord taskWithResources(int cpu, int memMb, int diskMb) {
        return taskWith(ExecutorType.SIMULATED, cpu, memMb, diskMb,
            TaskConstraints.unconstrained(), Priority.NORMAL);
    }

    static TaskRecord taskWithConstraints(Set<String> requiredTags,
                                           Set<String> blacklist, Set<String> whitelist) {
        return taskWith(ExecutorType.SIMULATED, 1, 256, 50,
            new TaskConstraints(requiredTags, blacklist, whitelist), Priority.NORMAL);
    }

    static TaskRecord anyTask() {
        return taskWithExecutor(ExecutorType.SIMULATED);
    }

    static TaskRecord taskWith(ExecutorType type, int cpu, int mem, int disk,
                                TaskConstraints constraints, Priority priority) {
        var descriptor = new TaskDescriptor(
            type, Map.of("durationMs", 100),
            new ResourceProfile(cpu, mem, disk, false, 10, false),
            constraints, priority, ExecutionPolicy.defaults(), TaskIO.none()
        );
        var record = TaskRecord.create(descriptor);
        record.transitionTo(TaskState.VALIDATED);
        record.transitionTo(TaskState.QUEUED);
        return record;
    }

    static WorkerRecord workerRecord(String id, WorkerHealthState state) {
        return new WorkerRecord(id, state,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 2048, 500, false, 0, false), Set.of()),
            Instant.now());
    }

    static WorkerRecord workerRecord(String id) {
        return workerRecord(id, WorkerHealthState.HEALTHY);
    }

    static WorkerRecord workerWithExecutors(String id, Set<ExecutorType> executors) {
        return new WorkerRecord(id, WorkerHealthState.HEALTHY,
            new WorkerCapabilities(executors,
                new ResourceProfile(4, 2048, 500, false, 0, false), Set.of()),
            Instant.now());
    }

    static WorkerRecord workerWithCapacity(String id, int totalCpu, int totalMem, int totalDisk,
                                            int allocCpu, int allocMem, int allocDisk) {
        var record = new WorkerRecord(id, WorkerHealthState.HEALTHY,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(totalCpu, totalMem, totalDisk, false, 0, false), Set.of()),
            Instant.now());
        if (allocCpu > 0 || allocMem > 0 || allocDisk > 0) {
            record.allocateResources(new ResourceProfile(allocCpu, allocMem, allocDisk, false, 0, false));
        }
        return record;
    }

    static WorkerRecord workerWithTags(String id, Set<String> tags) {
        return new WorkerRecord(id, WorkerHealthState.HEALTHY,
            new WorkerCapabilities(Set.of(ExecutorType.SIMULATED),
                new ResourceProfile(4, 2048, 500, false, 0, false), tags),
            Instant.now());
    }
}

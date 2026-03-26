package com.cloudbalancer.common.runtime;

import com.cloudbalancer.common.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalThreadRuntime implements NodeRuntime {

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();

    @Override
    public boolean startWorker(WorkerConfig config) {
        var capabilities = new WorkerCapabilities(
            config.supportedExecutors(),
            new ResourceProfile(config.cpuCores(), config.memoryMB(), config.diskMB(), false, 0, true),
            config.tags()
        );
        var metrics = new WorkerMetrics(0, 0, 0, 0, 0, 0L, 0L, 0.0, Instant.now());
        var info = new WorkerInfo(config.workerId(), WorkerHealthState.HEALTHY, capabilities, metrics, Instant.now());
        workers.put(config.workerId(), info);
        return true;
    }

    @Override
    public void stopWorker(String workerId) {
        workers.remove(workerId);
    }

    @Override
    public WorkerInfo getWorkerInfo(String workerId) {
        return workers.get(workerId);
    }

    @Override
    public List<WorkerInfo> listWorkers() {
        return List.copyOf(workers.values());
    }
}

package com.cloudbalancer.common.runtime;

import com.cloudbalancer.common.model.WorkerInfo;
import java.util.List;

public interface NodeRuntime {
    boolean startWorker(WorkerConfig config);
    void stopWorker(String workerId);
    WorkerInfo getWorkerInfo(String workerId);
    List<WorkerInfo> listWorkers();

    default void drainAndStop(String workerId, int drainTimeSeconds) {
        // Default no-op; DockerRuntime overrides with Kafka drain command + delayed stop
    }
}

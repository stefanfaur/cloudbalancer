package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.common.model.WorkerInfo;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkerRegistryService {

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public void registerWorker(WorkerInfo worker) {
        workers.put(worker.id(), worker);
    }

    public WorkerInfo getWorker(String workerId) {
        return workers.get(workerId);
    }

    public List<WorkerInfo> getAvailableWorkers() {
        return List.copyOf(workers.values());
    }

    public WorkerInfo nextWorkerRoundRobin() {
        List<WorkerInfo> available = new ArrayList<>(workers.values());
        if (available.isEmpty()) return null;
        int index = roundRobinIndex.getAndIncrement() % available.size();
        return available.get(index);
    }
}

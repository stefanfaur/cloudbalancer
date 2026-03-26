package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DispatcherLocalRuntime implements NodeRuntime {

    private static final Logger log = LoggerFactory.getLogger(DispatcherLocalRuntime.class);
    private final WorkerRegistryService workerRegistry;
    private final EventPublisher eventPublisher;
    private final ConcurrentHashMap<String, ExecutorService> workers = new ConcurrentHashMap<>();

    public DispatcherLocalRuntime(WorkerRegistryService workerRegistry, EventPublisher eventPublisher) {
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public boolean startWorker(WorkerConfig config) {
        var capabilities = new WorkerCapabilities(
            config.supportedExecutors(),
            new ResourceProfile(config.cpuCores(), config.memoryMB(), config.diskMB(), false, 0, true),
            config.tags());

        workerRegistry.registerWorker(config.workerId(), WorkerHealthState.HEALTHY, capabilities);

        var executor = Executors.newSingleThreadExecutor();
        workers.put(config.workerId(), executor);

        eventPublisher.publishEvent("workers.registration", config.workerId(),
            new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(),
                config.workerId(), capabilities));

        log.info("Local worker started: {}", config.workerId());
        return true;
    }

    @Override
    public void stopWorker(String workerId) {
        var executor = workers.remove(workerId);
        if (executor != null) {
            executor.shutdown();
        }
        log.info("Local worker stopped: {}", workerId);
    }

    @Override
    public WorkerInfo getWorkerInfo(String workerId) {
        var record = workerRegistry.getWorker(workerId);
        if (record == null) return null;
        return new WorkerInfo(record.getId(), record.getHealthState(),
            record.getCapabilities(), null, record.getRegisteredAt());
    }

    @Override
    public List<WorkerInfo> listWorkers() {
        return workers.keySet().stream()
            .map(this::getWorkerInfo)
            .filter(w -> w != null)
            .toList();
    }
}

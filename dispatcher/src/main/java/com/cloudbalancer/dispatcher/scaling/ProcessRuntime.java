package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.config.ScalingProperties;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ProcessRuntime implements NodeRuntime {

    private static final Logger log = LoggerFactory.getLogger(ProcessRuntime.class);
    private final WorkerRegistryService workerRegistry;
    private final EventPublisher eventPublisher;
    private final ScalingProperties props;
    private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

    public ProcessRuntime(WorkerRegistryService workerRegistry,
                          EventPublisher eventPublisher,
                          ScalingProperties props) {
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
        this.props = props;
    }

    @Override
    public boolean startWorker(WorkerConfig config) {
        try {
            List<String> command = buildCommand(config, props.getProcessWorkerJarPath());
            Map<String, String> env = buildEnvironment(config, props.getProcessWorkerKafkaBootstrap());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().putAll(env);
            pb.inheritIO();

            Process process = pb.start();
            processes.put(config.workerId(), process);

            var capabilities = new WorkerCapabilities(
                config.supportedExecutors(),
                new ResourceProfile(config.cpuCores(), config.memoryMB(), config.diskMB(), false, 0, true),
                config.tags());
            workerRegistry.registerWorker(config.workerId(), WorkerHealthState.HEALTHY, capabilities);

            eventPublisher.publishEvent("workers.registration", config.workerId(),
                new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(),
                    config.workerId(), capabilities));

            log.info("Process worker started: {} (PID: {})", config.workerId(), process.pid());
            return true;
        } catch (IOException e) {
            log.error("Failed to start process worker: {}", config.workerId(), e);
            return false;
        }
    }

    @Override
    public void stopWorker(String workerId) {
        Process process = processes.remove(workerId);
        if (process != null && process.isAlive()) {
            process.destroy();
            log.info("Process worker stopped: {}", workerId);
        }
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
        return processes.keySet().stream()
            .map(this::getWorkerInfo)
            .filter(Objects::nonNull)
            .toList();
    }

    // Static helpers for testability

    static List<String> buildCommand(WorkerConfig config, String jarPath) {
        return List.of(
            "java",
            "-Xmx" + config.memoryMB() + "m",
            "-jar", jarPath,
            "--cloudbalancer.worker.id=" + config.workerId()
        );
    }

    static Map<String, String> buildEnvironment(WorkerConfig config, String kafkaBootstrap) {
        Map<String, String> env = new HashMap<>();
        env.put("WORKER_ID", config.workerId());
        env.put("KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrap);
        env.put("SUPPORTED_EXECUTORS", config.supportedExecutors().stream()
            .map(Enum::name)
            .collect(Collectors.joining(",")));
        env.put("CPU_CORES", String.valueOf(config.cpuCores()));
        env.put("MEMORY_MB", String.valueOf(config.memoryMB()));
        env.put("DISK_MB", String.valueOf(config.diskMB()));
        if (!config.tags().isEmpty()) {
            env.put("TAGS", String.join(",", config.tags()));
        }
        return env;
    }
}

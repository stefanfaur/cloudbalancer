package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.dispatcher.config.ScalingProperties;
import com.cloudbalancer.dispatcher.kafka.EventPublisher;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class DockerRuntime implements NodeRuntime {

    private static final Logger log = LoggerFactory.getLogger(DockerRuntime.class);
    private static final String CONTAINER_PREFIX = "cloudbalancer-worker-";

    private final DockerClient dockerClient;
    private final WorkerRegistryService workerRegistry;
    private final EventPublisher eventPublisher;
    private final ScalingProperties props;
    private final ConcurrentHashMap<String, String> workerContainers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService drainScheduler = Executors.newSingleThreadScheduledExecutor(
        r -> { Thread t = new Thread(r, "drain-scheduler"); t.setDaemon(true); return t; }
    );

    public DockerRuntime(DockerClient dockerClient,
                         WorkerRegistryService workerRegistry,
                         EventPublisher eventPublisher,
                         ScalingProperties props) {
        this.dockerClient = dockerClient;
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
        this.props = props;
    }

    @PostConstruct
    public void reconcileOnStartup() {
        log.info("Reconciling Docker containers on startup...");
        try {
            var containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(CONTAINER_PREFIX + "auto-docker-"))
                .exec();

            for (var container : containers) {
                String containerName = container.getNames()[0].replace("/", "");
                String workerId = containerName.replace(CONTAINER_PREFIX, "");

                var workerRecord = workerRegistry.getWorker(workerId);

                if (workerRecord == null || workerRecord.getHealthState() == WorkerHealthState.DEAD) {
                    log.info("Removing orphaned container: {} (worker: {})", containerName, workerId);
                    safeStopAndRemove(container.getId());
                } else if (workerRecord.getHealthState() == WorkerHealthState.DRAINING) {
                    workerContainers.put(workerId, container.getId());
                    Instant drainStarted = workerRecord.getDrainStartedAt();
                    if (drainStarted != null) {
                        long elapsed = Duration.between(drainStarted, Instant.now()).getSeconds();
                        long remaining = props.getDrainTimeSeconds() - elapsed;
                        if (remaining <= 0) {
                            log.info("Drain time elapsed for worker {}, stopping now", workerId);
                            stopWorker(workerId);
                        } else {
                            log.info("Re-scheduling drain stop for worker {} in {}s", workerId, remaining);
                            drainScheduler.schedule(() -> stopWorker(workerId), remaining, TimeUnit.SECONDS);
                        }
                    } else {
                        stopWorker(workerId);
                    }
                } else {
                    workerContainers.put(workerId, container.getId());
                    log.info("Re-tracked container for worker {}", workerId);
                }
            }
            log.info("Reconciliation complete: tracking {} containers", workerContainers.size());
        } catch (Exception e) {
            log.error("Failed to reconcile Docker containers on startup", e);
        }
    }

    @Override
    public boolean startWorker(WorkerConfig config) {
        try {
            String containerName = CONTAINER_PREFIX + config.workerId();

            HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
                .withMemory(config.memoryMB() * 1024L * 1024L)
                .withNanoCPUs((long) config.cpuCores() * 1_000_000_000L)
                .withNetworkMode(props.getDockerNetworkName())
                .withPrivileged(true);

            CreateContainerResponse response = dockerClient.createContainerCmd(props.getDockerWorkerImage())
                .withName(containerName)
                .withEnv(
                    "WORKER_ID=" + config.workerId(),
                    "KAFKA_BOOTSTRAP_SERVERS=" + props.getKafkaBootstrapInternal(),
                    "DISPATCHER_URL=http://dispatcher:8080"
                )
                .withHostConfig(hostConfig)
                .exec();

            dockerClient.startContainerCmd(response.getId()).exec();

            workerContainers.put(config.workerId(), response.getId());

            var capabilities = new WorkerCapabilities(
                config.supportedExecutors(),
                new ResourceProfile(config.cpuCores(), config.memoryMB(), config.diskMB(), false, 0, true),
                config.tags());
            workerRegistry.registerWorker(config.workerId(), WorkerHealthState.HEALTHY, capabilities);

            eventPublisher.publishEvent("workers.registration", config.workerId(),
                new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(),
                    config.workerId(), capabilities));

            log.info("Docker worker started: {} (container: {})", config.workerId(), response.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to start Docker worker: {}", config.workerId(), e);
            return false;
        }
    }

    @Override
    public void drainAndStop(String workerId, int drainTimeSeconds) {
        var drainCommand = new DrainCommand(workerId, drainTimeSeconds, Instant.now());
        eventPublisher.publishMessage("workers.commands", workerId, drainCommand);
        log.info("Published DrainCommand for worker {}, scheduling stop in {}s", workerId, drainTimeSeconds);

        drainScheduler.schedule(() -> stopWorker(workerId), drainTimeSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stopWorker(String workerId) {
        String containerId = workerContainers.remove(workerId);
        if (containerId == null) {
            log.warn("No container tracked for worker {}, skipping stop", workerId);
            return;
        }
        safeStopAndRemove(containerId);
        workerRegistry.markDead(workerId);
        log.info("Docker worker stopped and removed: {}", workerId);
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
        return workerContainers.keySet().stream()
            .map(this::getWorkerInfo)
            .filter(Objects::nonNull)
            .toList();
    }

    @PreDestroy
    public void shutdown() {
        drainScheduler.shutdownNow();
    }

    private void safeStopAndRemove(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(10).exec();
        } catch (Exception e) {
            log.warn("Failed to stop container {}: {}", containerId, e.getMessage());
        }
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        }
    }
}

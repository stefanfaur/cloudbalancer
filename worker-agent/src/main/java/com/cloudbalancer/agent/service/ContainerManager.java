package com.cloudbalancer.agent.service;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.registration.AgentRegistrationClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerManager {

    private static final Logger log = LoggerFactory.getLogger(ContainerManager.class);
    private static final String CONTAINER_PREFIX = "cloudbalancer-worker-";

    private final DockerClient dockerClient;
    private final AgentProperties props;
    private final AgentRegistrationClient registrationClient;
    private final ConcurrentHashMap<String, String> workerContainers = new ConcurrentHashMap<>();

    public ContainerManager(DockerClient dockerClient, AgentProperties props,
                            AgentRegistrationClient registrationClient) {
        this.dockerClient = dockerClient;
        this.props = props;
        this.registrationClient = registrationClient;
    }

    @PostConstruct
    public void reconcileOnStartup() {
        log.info("Agent {} reconciling containers on startup — killing orphans", props.getId());
        try {
            var containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(List.of(CONTAINER_PREFIX))
                .exec();

            for (var container : containers) {
                String containerName = container.getNames()[0].replace("/", "");
                log.info("Removing orphaned container: {}", containerName);
                safeStopAndRemove(container.getId());
            }
            log.info("Reconciliation complete: removed {} orphaned containers", containers.size());
        } catch (Exception e) {
            log.error("Failed to reconcile containers on startup", e);
        }
    }

    public String startWorker(String workerId, int cpuCores, int memoryMB, String... envVars) {
        String containerName = CONTAINER_PREFIX + workerId;

        HostConfig hostConfig = HostConfig.newHostConfig()
            .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
            .withMemory(memoryMB * 1024L * 1024L)
            .withNanoCPUs((long) cpuCores * 1_000_000_000L)
            .withNetworkMode(props.getDocker().getNetworkName())
            .withPrivileged(true);

        List<String> env = new ArrayList<>(List.of(envVars));
        var regResult = registrationClient.getCachedResult();
        if (regResult != null) {
            env.add("KAFKA_BOOTSTRAP_SERVERS=" + regResult.kafkaBootstrap());
            env.add("KAFKA_SECURITY_PROTOCOL=SASL_PLAINTEXT");
            env.add("KAFKA_SASL_MECHANISM=PLAIN");
            env.add("KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + regResult.kafkaUsername() + "\" password=\"" + regResult.kafkaPassword() + "\";");
            env.add("DISPATCHER_URL=" + props.getDispatcherUrl());
        } else {
            env.add("KAFKA_BOOTSTRAP_SERVERS=" + props.getDocker().getKafkaBootstrapInternal());
            env.add("DISPATCHER_URL=http://dispatcher:8080");
        }

        CreateContainerResponse response = dockerClient.createContainerCmd(props.getDocker().getWorkerImage())
            .withName(containerName)
            .withEnv(env)
            .withHostConfig(hostConfig)
            .exec();

        dockerClient.startContainerCmd(response.getId()).exec();
        workerContainers.put(workerId, response.getId());

        log.info("Started container {} for worker {}", response.getId(), workerId);
        return response.getId();
    }

    public void stopWorker(String workerId) {
        String containerId = workerContainers.remove(workerId);
        if (containerId == null) {
            log.warn("No container tracked for worker {}, skipping stop", workerId);
            return;
        }
        safeStopAndRemove(containerId);
        log.info("Stopped and removed container for worker {}", workerId);
    }

    public List<String> getActiveWorkerIds() {
        return List.copyOf(workerContainers.keySet());
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

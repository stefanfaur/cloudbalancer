package com.cloudbalancer.agent.config;

import com.cloudbalancer.common.model.ExecutorType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.agent")
public class AgentProperties {
    private String id = "agent-1";
    private String hostname = "localhost";
    private double totalCpuCores = 8;
    private long totalMemoryMb = 16384;
    private Set<ExecutorType> supportedExecutors = Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL, ExecutorType.DOCKER);
    private long heartbeatIntervalMs = 10000;
    private String registrationToken;
    private String dispatcherUrl = "http://dispatcher:8080";
    private DockerProperties docker = new DockerProperties();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public double getTotalCpuCores() { return totalCpuCores; }
    public void setTotalCpuCores(double totalCpuCores) { this.totalCpuCores = totalCpuCores; }
    public long getTotalMemoryMb() { return totalMemoryMb; }
    public void setTotalMemoryMb(long totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }
    public Set<ExecutorType> getSupportedExecutors() { return supportedExecutors; }
    public void setSupportedExecutors(Set<ExecutorType> supportedExecutors) { this.supportedExecutors = supportedExecutors; }
    public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) { this.heartbeatIntervalMs = heartbeatIntervalMs; }
    public String getRegistrationToken() { return registrationToken; }
    public void setRegistrationToken(String registrationToken) { this.registrationToken = registrationToken; }
    public String getDispatcherUrl() { return dispatcherUrl; }
    public void setDispatcherUrl(String dispatcherUrl) { this.dispatcherUrl = dispatcherUrl; }
    public DockerProperties getDocker() { return docker; }
    public void setDocker(DockerProperties docker) { this.docker = docker; }

    public static class DockerProperties {
        private String host = "unix:///var/run/docker.sock";
        private String workerImage = "docker-worker";
        private String networkName = "docker_default";
        private String kafkaBootstrapInternal = "kafka:29092";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getWorkerImage() { return workerImage; }
        public void setWorkerImage(String workerImage) { this.workerImage = workerImage; }
        public String getNetworkName() { return networkName; }
        public void setNetworkName(String networkName) { this.networkName = networkName; }
        public String getKafkaBootstrapInternal() { return kafkaBootstrapInternal; }
        public void setKafkaBootstrapInternal(String kafkaBootstrapInternal) { this.kafkaBootstrapInternal = kafkaBootstrapInternal; }
    }
}

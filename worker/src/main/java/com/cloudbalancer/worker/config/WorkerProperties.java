package com.cloudbalancer.worker.config;

import com.cloudbalancer.common.model.ExecutorType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "cloudbalancer.worker")
public class WorkerProperties {
    private String id = "worker-1";
    private Set<ExecutorType> supportedExecutors = Set.of(ExecutorType.SIMULATED);
    private int cpuCores = 4;
    private int memoryMb = 8192;
    private int diskMb = 10240;
    private Set<String> tags = Set.of();
    private long metricsIntervalMs = 5000;
    private long heartbeatIntervalMs = 10000;
    private ShellConfig shell = new ShellConfig();
    private DockerConfig docker = new DockerConfig();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Set<ExecutorType> getSupportedExecutors() { return supportedExecutors; }
    public void setSupportedExecutors(Set<ExecutorType> supportedExecutors) { this.supportedExecutors = supportedExecutors; }
    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
    public int getMemoryMb() { return memoryMb; }
    public void setMemoryMb(int memoryMb) { this.memoryMb = memoryMb; }
    public int getDiskMb() { return diskMb; }
    public void setDiskMb(int diskMb) { this.diskMb = diskMb; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public long getMetricsIntervalMs() { return metricsIntervalMs; }
    public void setMetricsIntervalMs(long metricsIntervalMs) { this.metricsIntervalMs = metricsIntervalMs; }
    public long getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) { this.heartbeatIntervalMs = heartbeatIntervalMs; }
    public ShellConfig getShell() { return shell; }
    public void setShell(ShellConfig shell) { this.shell = shell; }
    public DockerConfig getDocker() { return docker; }
    public void setDocker(DockerConfig docker) { this.docker = docker; }

    public static class ShellConfig {
        private Set<String> blockedCommands = Set.of(
            "rm -rf /", "shutdown", "reboot", "mkfs", "dd", ":(){ :|:& };:"
        );
        private int maxOutputBytes = 1_048_576;

        public Set<String> getBlockedCommands() { return blockedCommands; }
        public void setBlockedCommands(Set<String> blockedCommands) { this.blockedCommands = blockedCommands; }
        public int getMaxOutputBytes() { return maxOutputBytes; }
        public void setMaxOutputBytes(int maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    }

    public static class DockerConfig {
        private String host = "unix:///var/run/docker.sock";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
    }
}

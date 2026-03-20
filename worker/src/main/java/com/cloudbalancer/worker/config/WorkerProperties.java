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
}

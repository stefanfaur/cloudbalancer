package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.model.ExecutorType;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class AgentInfo {
    private final String agentId;
    private final String hostname;
    private double totalCpuCores;
    private double availableCpuCores;
    private long totalMemoryMB;
    private long availableMemoryMB;
    private List<String> activeWorkerIds;
    private Set<ExecutorType> supportedExecutors;
    private Instant lastHeartbeat;

    public AgentInfo(String agentId, String hostname) {
        this.agentId = agentId;
        this.hostname = hostname;
        this.lastHeartbeat = Instant.now();
    }

    public String agentId() { return agentId; }
    public String hostname() { return hostname; }
    public double totalCpuCores() { return totalCpuCores; }
    public double availableCpuCores() { return availableCpuCores; }
    public long totalMemoryMB() { return totalMemoryMB; }
    public long availableMemoryMB() { return availableMemoryMB; }
    public List<String> activeWorkerIds() { return activeWorkerIds; }
    public Set<ExecutorType> supportedExecutors() { return supportedExecutors; }
    public Instant lastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public void updateFrom(com.cloudbalancer.common.agent.AgentHeartbeat hb) {
        this.totalCpuCores = hb.totalCpuCores();
        this.availableCpuCores = hb.availableCpuCores();
        this.totalMemoryMB = hb.totalMemoryMB();
        this.availableMemoryMB = hb.availableMemoryMB();
        this.activeWorkerIds = hb.activeWorkerIds();
        this.supportedExecutors = hb.supportedExecutors();
        this.lastHeartbeat = Instant.now();
    }
}

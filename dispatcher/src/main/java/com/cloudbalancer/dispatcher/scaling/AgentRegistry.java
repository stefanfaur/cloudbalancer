package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.runtime.WorkerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);
    private final ConcurrentHashMap<String, AgentInfo> agents = new ConcurrentHashMap<>();

    public void updateAgent(AgentHeartbeat heartbeat) {
        agents.compute(heartbeat.agentId(), (id, existing) -> {
            if (existing == null) {
                var info = new AgentInfo(heartbeat.agentId(), heartbeat.hostname());
                info.updateFrom(heartbeat);
                log.info("Registered new agent: {} ({})", id, heartbeat.hostname());
                return info;
            }
            existing.updateFrom(heartbeat);
            return existing;
        });
    }

    public void markDeadIfStale(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        agents.entrySet().removeIf(entry -> {
            if (entry.getValue().lastHeartbeat().isBefore(cutoff)) {
                log.warn("Agent {} marked DEAD (last heartbeat: {})", entry.getKey(), entry.getValue().lastHeartbeat());
                return true;
            }
            return false;
        });
    }

    public Optional<AgentInfo> selectBestHost(WorkerConfig config) {
        return agents.values().stream()
            .filter(a -> a.availableCpuCores() >= config.cpuCores())
            .filter(a -> a.availableMemoryMB() >= config.memoryMB())
            .filter(a -> a.supportedExecutors() != null && a.supportedExecutors().containsAll(config.supportedExecutors()))
            .max(Comparator.comparingDouble(AgentInfo::availableCpuCores));
    }

    public List<AgentInfo> getAliveAgents() {
        return List.copyOf(agents.values());
    }

    public Optional<AgentInfo> getAgent(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    public void removeAgent(String agentId) {
        agents.remove(agentId);
    }
}

package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.agent.AgentCommand;
import com.cloudbalancer.common.model.WorkerInfo;
import com.cloudbalancer.common.runtime.NodeRuntime;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AgentRuntime implements NodeRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntime.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AgentRegistry agentRegistry;
    private final PendingWorkerTracker pendingTracker;

    public AgentRuntime(KafkaTemplate<String, String> kafkaTemplate,
                        AgentRegistry agentRegistry,
                        PendingWorkerTracker pendingTracker) {
        this.kafkaTemplate = kafkaTemplate;
        this.agentRegistry = agentRegistry;
        this.pendingTracker = pendingTracker;
    }

    @Override
    public boolean startWorker(WorkerConfig config) {
        var agent = agentRegistry.selectBestHost(config);
        if (agent.isEmpty()) {
            log.warn("No agent with sufficient capacity for worker {}", config.workerId());
            return false;
        }

        var agentInfo = agent.get();
        var cmd = new AgentCommand.StartWorkerCommand(
            agentInfo.agentId(), config.workerId(),
            config.cpuCores(), config.memoryMB(), config.diskMB(),
            config.supportedExecutors(), config.tags(), Map.of());

        try {
            String json = JsonUtil.mapper().writeValueAsString(cmd);
            kafkaTemplate.send("agents.commands", agentInfo.agentId(), json);
            pendingTracker.markPending(config.workerId(), agentInfo.agentId(), Instant.now());
            log.info("Sent StartWorkerCommand for {} to agent {}", config.workerId(), agentInfo.agentId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send StartWorkerCommand for {}", config.workerId(), e);
            return false;
        }
    }

    @Override
    public void stopWorker(String workerId) {
        sendStopCommand(workerId, false, 0);
    }

    @Override
    public void drainAndStop(String workerId, int drainTimeSeconds) {
        sendStopCommand(workerId, true, drainTimeSeconds);
    }

    @Override
    public WorkerInfo getWorkerInfo(String workerId) {
        return null; // Worker info comes from WorkerRegistryService, not the runtime
    }

    @Override
    public List<WorkerInfo> listWorkers() {
        return agentRegistry.getAliveAgents().stream()
            .flatMap(a -> a.activeWorkerIds() != null ? a.activeWorkerIds().stream() : java.util.stream.Stream.empty())
            .map(this::getWorkerInfo)
            .filter(Objects::nonNull)
            .toList();
    }

    public int getPendingWorkerCount() {
        return pendingTracker.pendingCount();
    }

    private void sendStopCommand(String workerId, boolean drain, int drainTimeSeconds) {
        String agentId = findAgentForWorker(workerId);
        if (agentId == null) {
            log.warn("No agent found for worker {}, cannot send stop command", workerId);
            return;
        }

        var cmd = new AgentCommand.StopWorkerCommand(agentId, workerId, drain, drainTimeSeconds);
        try {
            String json = JsonUtil.mapper().writeValueAsString(cmd);
            kafkaTemplate.send("agents.commands", agentId, json);
            log.info("Sent StopWorkerCommand for {} to agent {} (drain={})", workerId, agentId, drain);
        } catch (Exception e) {
            log.error("Failed to send StopWorkerCommand for {}", workerId, e);
        }
    }

    private String findAgentForWorker(String workerId) {
        String agentId = pendingTracker.getAgentForWorker(workerId);
        if (agentId != null) return agentId;

        return agentRegistry.getAliveAgents().stream()
            .filter(a -> a.activeWorkerIds() != null && a.activeWorkerIds().contains(workerId))
            .map(AgentInfo::agentId)
            .findFirst()
            .orElse(null);
    }
}

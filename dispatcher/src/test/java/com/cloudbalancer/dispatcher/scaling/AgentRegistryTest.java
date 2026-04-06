package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.runtime.WorkerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
    }

    @Test
    void updateAgentRegistersNewAgent() {
        registry.updateAgent(heartbeat("agent-1", 8.0, 6.0, 16384, 12288));
        assertThat(registry.getAliveAgents()).hasSize(1);
    }

    @Test
    void selectBestHostPicksAgentWithMostCapacity() {
        registry.updateAgent(heartbeat("agent-1", 8.0, 2.0, 16384, 4096));
        registry.updateAgent(heartbeat("agent-2", 8.0, 6.0, 16384, 12288));

        var config = new WorkerConfig("w-1", Set.of(ExecutorType.DOCKER), 4, 8192, 10240, Set.of());
        var selected = registry.selectBestHost(config);

        assertThat(selected).isPresent();
        assertThat(selected.get().agentId()).isEqualTo("agent-2");
    }

    @Test
    void selectBestHostReturnsEmptyWhenNoCapacity() {
        registry.updateAgent(heartbeat("agent-1", 8.0, 1.0, 16384, 2048));

        var config = new WorkerConfig("w-1", Set.of(ExecutorType.DOCKER), 4, 8192, 10240, Set.of());
        assertThat(registry.selectBestHost(config)).isEmpty();
    }

    @Test
    void markDeadIfStaleRemovesStaleAgents() {
        registry.updateAgent(heartbeat("agent-1", 8.0, 6.0, 16384, 12288));
        registry.getAliveAgents().get(0).setLastHeartbeat(Instant.now().minus(Duration.ofSeconds(60)));

        registry.markDeadIfStale(Duration.ofSeconds(30));
        assertThat(registry.getAliveAgents()).isEmpty();
    }

    private AgentHeartbeat heartbeat(String agentId, double totalCpu, double availCpu, long totalMem, long availMem) {
        return new AgentHeartbeat(agentId, "host-" + agentId, totalCpu, availCpu, totalMem, availMem,
            List.of(), Set.of(ExecutorType.DOCKER, ExecutorType.SHELL), Instant.now());
    }
}

package com.cloudbalancer.dispatcher.scaling;

import com.cloudbalancer.common.agent.AgentCommand;
import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.runtime.WorkerConfig;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgentRuntimeTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private AgentRegistry agentRegistry;
    private PendingWorkerTracker pendingTracker;
    private AgentRuntime agentRuntime;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        agentRegistry = new AgentRegistry();
        pendingTracker = new PendingWorkerTracker();
        agentRuntime = new AgentRuntime(kafkaTemplate, agentRegistry, pendingTracker);
    }

    @Test
    void startWorkerSelectsAgentAndPublishesCommand() throws Exception {
        agentRegistry.updateAgent(new AgentHeartbeat("agent-1", "host-1", 8.0, 6.0, 16384, 12288,
            List.of(), Set.of(ExecutorType.DOCKER), Instant.now()));

        var config = new WorkerConfig("worker-5", Set.of(ExecutorType.DOCKER), 4, 8192, 10240, Set.of());
        boolean result = agentRuntime.startWorker(config);

        assertThat(result).isTrue();
        assertThat(pendingTracker.pendingCount()).isEqualTo(1);

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.commands"), eq("agent-1"), valueCaptor.capture());

        var cmd = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentCommand.class);
        assertThat(cmd).isInstanceOf(AgentCommand.StartWorkerCommand.class);
        assertThat(((AgentCommand.StartWorkerCommand) cmd).workerId()).isEqualTo("worker-5");
    }

    @Test
    void startWorkerReturnsFalseWhenNoAgentAvailable() {
        var config = new WorkerConfig("worker-5", Set.of(ExecutorType.DOCKER), 4, 8192, 10240, Set.of());
        boolean result = agentRuntime.startWorker(config);

        assertThat(result).isFalse();
        assertThat(pendingTracker.pendingCount()).isZero();
    }

    @Test
    void stopWorkerPublishesStopCommand() throws Exception {
        agentRegistry.updateAgent(new AgentHeartbeat("agent-1", "host-1", 8.0, 6.0, 16384, 12288,
            List.of("worker-5"), Set.of(ExecutorType.DOCKER), Instant.now()));

        agentRuntime.stopWorker("worker-5");

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.commands"), eq("agent-1"), valueCaptor.capture());

        var cmd = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentCommand.class);
        assertThat(cmd).isInstanceOf(AgentCommand.StopWorkerCommand.class);
        assertThat(((AgentCommand.StopWorkerCommand) cmd).drain()).isFalse();
    }

    @Test
    void drainAndStopPublishesDrainCommand() throws Exception {
        agentRegistry.updateAgent(new AgentHeartbeat("agent-1", "host-1", 8.0, 6.0, 16384, 12288,
            List.of("worker-5"), Set.of(ExecutorType.DOCKER), Instant.now()));

        agentRuntime.drainAndStop("worker-5", 60);

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.commands"), eq("agent-1"), valueCaptor.capture());

        var cmd = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentCommand.class);
        assertThat(cmd).isInstanceOf(AgentCommand.StopWorkerCommand.class);
        var stop = (AgentCommand.StopWorkerCommand) cmd;
        assertThat(stop.drain()).isTrue();
        assertThat(stop.drainTimeSeconds()).isEqualTo(60);
    }
}

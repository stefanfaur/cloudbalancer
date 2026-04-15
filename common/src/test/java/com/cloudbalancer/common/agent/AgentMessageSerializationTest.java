package com.cloudbalancer.common.agent;

import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentMessageSerializationTest {

    @Test
    void agentHeartbeatRoundTrip() throws Exception {
        var hb = new AgentHeartbeat("agent-1", "host-1", 8.0, 4.0, 16384, 8192,
            List.of("worker-1"), Set.of(ExecutorType.DOCKER, ExecutorType.SHELL), Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(hb);
        var deserialized = JsonUtil.mapper().readValue(json, AgentHeartbeat.class);

        assertThat(deserialized.agentId()).isEqualTo("agent-1");
        assertThat(deserialized.availableCpuCores()).isEqualTo(4.0);
        assertThat(deserialized.activeWorkerIds()).containsExactly("worker-1");
    }

    @Test
    void startWorkerCommandRoundTrip() throws Exception {
        AgentCommand cmd = new AgentCommand.StartWorkerCommand(
            "agent-1", "worker-5", 4, 8192, 10240,
            Set.of(ExecutorType.DOCKER), Set.of(), Map.of("FOO", "BAR"));

        String json = JsonUtil.mapper().writeValueAsString(cmd);
        var deserialized = JsonUtil.mapper().readValue(json, AgentCommand.class);

        assertThat(deserialized).isInstanceOf(AgentCommand.StartWorkerCommand.class);
        var start = (AgentCommand.StartWorkerCommand) deserialized;
        assertThat(start.workerId()).isEqualTo("worker-5");
        assertThat(start.environment()).containsEntry("FOO", "BAR");
    }

    @Test
    void stopWorkerCommandRoundTrip() throws Exception {
        AgentCommand cmd = new AgentCommand.StopWorkerCommand("agent-1", "worker-5", true, 60);

        String json = JsonUtil.mapper().writeValueAsString(cmd);
        var deserialized = JsonUtil.mapper().readValue(json, AgentCommand.class);

        assertThat(deserialized).isInstanceOf(AgentCommand.StopWorkerCommand.class);
        var stop = (AgentCommand.StopWorkerCommand) deserialized;
        assertThat(stop.drain()).isTrue();
        assertThat(stop.drainTimeSeconds()).isEqualTo(60);
    }

    @Test
    void workerStartedEventRoundTrip() throws Exception {
        AgentEvent event = new AgentEvent.WorkerStartedEvent("agent-1", "worker-5", "abc123", Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(event);
        var deserialized = JsonUtil.mapper().readValue(json, AgentEvent.class);

        assertThat(deserialized).isInstanceOf(AgentEvent.WorkerStartedEvent.class);
        assertThat(((AgentEvent.WorkerStartedEvent) deserialized).containerId()).isEqualTo("abc123");
    }

    @Test
    void workerStartFailedEventRoundTrip() throws Exception {
        AgentEvent event = new AgentEvent.WorkerStartFailedEvent("agent-1", "worker-5", "image not found", Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(event);
        var deserialized = JsonUtil.mapper().readValue(json, AgentEvent.class);

        assertThat(deserialized).isInstanceOf(AgentEvent.WorkerStartFailedEvent.class);
    }

    @Test
    void workerStoppedEventRoundTrip() throws Exception {
        AgentEvent event = new AgentEvent.WorkerStoppedEvent("agent-1", "worker-5", Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(event);
        var deserialized = JsonUtil.mapper().readValue(json, AgentEvent.class);

        assertThat(deserialized).isInstanceOf(AgentEvent.WorkerStoppedEvent.class);
    }

    @Test
    void containerCreatingEventRoundTrip() throws Exception {
        var event = new AgentEvent.ContainerCreatingEvent("agent-1", "worker-5", Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"CONTAINER_CREATING\"");

        var deserialized = JsonUtil.mapper().readValue(json, AgentEvent.class);
        assertThat(deserialized).isInstanceOf(AgentEvent.ContainerCreatingEvent.class);
        assertThat(((AgentEvent.ContainerCreatingEvent) deserialized).workerId()).isEqualTo("worker-5");
    }

    @Test
    void agentRegisteredEventRoundTrip() throws Exception {
        var event = new AgentRegisteredEvent("agent-1", "host-1", 8.0, 16384,
            Set.of(ExecutorType.DOCKER, ExecutorType.SHELL), Instant.now());

        String json = JsonUtil.mapper().writeValueAsString(event);
        var deserialized = JsonUtil.mapper().readValue(json, AgentRegisteredEvent.class);

        assertThat(deserialized.agentId()).isEqualTo("agent-1");
        assertThat(deserialized.totalCpuCores()).isEqualTo(8.0);
    }
}

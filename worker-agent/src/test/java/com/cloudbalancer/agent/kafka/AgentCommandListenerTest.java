package com.cloudbalancer.agent.kafka;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.service.ContainerManager;
import com.cloudbalancer.common.agent.AgentCommand;
import com.cloudbalancer.common.agent.AgentEvent;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentCommandListenerTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ContainerManager containerManager;
    private AgentCommandListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        containerManager = mock(ContainerManager.class);
        var props = new AgentProperties();
        props.setId("agent-1");
        listener = new AgentCommandListener(kafkaTemplate, containerManager, props);
    }

    @Test
    void startWorkerCommandCreatesContainerAndPublishesEvent() throws Exception {
        when(containerManager.startWorker(anyString(), anyInt(), anyInt(), any()))
            .thenReturn("container-abc");

        AgentCommand cmd = new AgentCommand.StartWorkerCommand(
            "agent-1", "worker-5", 4, 8192, 10240,
            Set.of(ExecutorType.DOCKER), Set.of(), Map.of());

        String json = JsonUtil.mapper().writeValueAsString(cmd);
        listener.onCommand(json);

        verify(containerManager).startWorker(eq("worker-5"), eq(4), eq(8192), any());

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.events"), eq("agent-1"), valueCaptor.capture());

        var event = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentEvent.class);
        assertThat(event).isInstanceOf(AgentEvent.WorkerStartedEvent.class);
        assertThat(((AgentEvent.WorkerStartedEvent) event).containerId()).isEqualTo("container-abc");
    }

    @Test
    void startWorkerCommandPublishesFailedEventOnError() throws Exception {
        when(containerManager.startWorker(anyString(), anyInt(), anyInt(), any()))
            .thenThrow(new RuntimeException("image not found"));

        AgentCommand cmd = new AgentCommand.StartWorkerCommand(
            "agent-1", "worker-5", 4, 8192, 10240,
            Set.of(ExecutorType.DOCKER), Set.of(), Map.of());

        listener.onCommand(JsonUtil.mapper().writeValueAsString(cmd));

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.events"), eq("agent-1"), valueCaptor.capture());

        var event = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentEvent.class);
        assertThat(event).isInstanceOf(AgentEvent.WorkerStartFailedEvent.class);
        assertThat(((AgentEvent.WorkerStartFailedEvent) event).reason()).contains("image not found");
    }

    @Test
    void commandForDifferentAgentIsIgnored() throws Exception {
        AgentCommand cmd = new AgentCommand.StartWorkerCommand(
            "agent-2", "worker-5", 4, 8192, 10240,
            Set.of(ExecutorType.DOCKER), Set.of(), Map.of());

        listener.onCommand(JsonUtil.mapper().writeValueAsString(cmd));

        verifyNoInteractions(containerManager);
    }

    @Test
    void stopWorkerCommandStopsContainerAndPublishesEvent() throws Exception {
        AgentCommand cmd = new AgentCommand.StopWorkerCommand("agent-1", "worker-5", false, 0);

        listener.onCommand(JsonUtil.mapper().writeValueAsString(cmd));

        verify(containerManager).stopWorker("worker-5");

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.events"), eq("agent-1"), valueCaptor.capture());

        var event = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentEvent.class);
        assertThat(event).isInstanceOf(AgentEvent.WorkerStoppedEvent.class);
    }
}

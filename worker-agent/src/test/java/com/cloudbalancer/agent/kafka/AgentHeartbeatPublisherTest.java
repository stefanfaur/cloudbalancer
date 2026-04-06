package com.cloudbalancer.agent.kafka;

import com.cloudbalancer.agent.config.AgentProperties;
import com.cloudbalancer.agent.service.ContainerManager;
import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.agent.AgentRegisteredEvent;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgentHeartbeatPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private AgentProperties props;
    private ContainerManager containerManager;
    private AgentHeartbeatPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        containerManager = mock(ContainerManager.class);
        props = new AgentProperties();
        props.setId("agent-1");
        props.setHostname("host-1");
        props.setTotalCpuCores(8);
        props.setTotalMemoryMb(16384);
        props.setSupportedExecutors(Set.of(ExecutorType.DOCKER, ExecutorType.SHELL));
        when(containerManager.getActiveWorkerIds()).thenReturn(List.of());

        publisher = new AgentHeartbeatPublisher(kafkaTemplate, props, containerManager);
    }

    @Test
    void publishRegistrationSendsToCorrectTopic() throws Exception {
        publisher.publishRegistration();

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.registration"), eq("agent-1"), valueCaptor.capture());

        var event = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentRegisteredEvent.class);
        assertThat(event.agentId()).isEqualTo("agent-1");
        assertThat(event.totalCpuCores()).isEqualTo(8.0);
    }

    @Test
    void publishHeartbeatIncludesCapacityInfo() throws Exception {
        when(containerManager.getActiveWorkerIds()).thenReturn(List.of("worker-1", "worker-2"));

        publisher.publishHeartbeat();

        var valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("agents.heartbeat"), eq("agent-1"), valueCaptor.capture());

        var hb = JsonUtil.mapper().readValue(valueCaptor.getValue(), AgentHeartbeat.class);
        assertThat(hb.agentId()).isEqualTo("agent-1");
        assertThat(hb.activeWorkerIds()).containsExactly("worker-1", "worker-2");
    }
}

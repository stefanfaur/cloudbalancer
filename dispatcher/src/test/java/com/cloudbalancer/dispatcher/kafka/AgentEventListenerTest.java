package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.agent.AgentEvent;
import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.model.ExecutorType;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.scaling.AgentRegistry;
import com.cloudbalancer.dispatcher.scaling.PendingWorkerTracker;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentEventListenerTest {

    private AgentRegistry agentRegistry;
    private PendingWorkerTracker pendingTracker;
    private WorkerRegistryService workerRegistry;
    private AgentEventListener listener;

    @BeforeEach
    void setUp() {
        agentRegistry = new AgentRegistry();
        pendingTracker = new PendingWorkerTracker();
        workerRegistry = mock(WorkerRegistryService.class);
        listener = new AgentEventListener(agentRegistry, pendingTracker, workerRegistry);
    }

    @Test
    void heartbeatUpdatesAgentRegistry() throws Exception {
        var hb = new AgentHeartbeat("agent-1", "host-1", 8.0, 6.0, 16384, 12288,
            List.of(), Set.of(ExecutorType.DOCKER), Instant.now());

        listener.onHeartbeat(JsonUtil.mapper().writeValueAsString(hb));

        assertThat(agentRegistry.getAliveAgents()).hasSize(1);
        assertThat(agentRegistry.getAliveAgents().get(0).agentId()).isEqualTo("agent-1");
    }

    @Test
    void workerStartedEventResolvesTrackerWithoutRegisteringWorker() throws Exception {
        pendingTracker.markPending("worker-5", "agent-1", Instant.now());

        var event = new AgentEvent.WorkerStartedEvent("agent-1", "worker-5", "abc123", Instant.now());
        listener.onAgentEvent(JsonUtil.mapper().writeValueAsString(event));

        assertThat(pendingTracker.pendingCount()).isZero();
        // Container start != worker ready. Registering here lets the scheduler
        // assign tasks before the worker's Kafka consumer joins, losing the
        // assignment (consumer starts at latest offset). The worker registers
        // itself on workers.registration once its consumer is ready.
        verify(workerRegistry, never()).registerWorker(any(), any(), any());
    }

    @Test
    void workerStartFailedEventResolvesTracker() throws Exception {
        pendingTracker.markPending("worker-5", "agent-1", Instant.now());

        var event = new AgentEvent.WorkerStartFailedEvent("agent-1", "worker-5", "image not found", Instant.now());
        listener.onAgentEvent(JsonUtil.mapper().writeValueAsString(event));

        assertThat(pendingTracker.pendingCount()).isZero();
    }

    @Test
    void workerStoppedEventMarksWorkerDead() throws Exception {
        var event = new AgentEvent.WorkerStoppedEvent("agent-1", "worker-5", Instant.now());
        listener.onAgentEvent(JsonUtil.mapper().writeValueAsString(event));

        verify(workerRegistry).markDead("worker-5");
    }
}

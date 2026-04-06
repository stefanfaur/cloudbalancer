package com.cloudbalancer.dispatcher.kafka;

import com.cloudbalancer.common.agent.AgentEvent;
import com.cloudbalancer.common.agent.AgentHeartbeat;
import com.cloudbalancer.common.agent.AgentRegisteredEvent;
import com.cloudbalancer.common.event.WorkerRegisteredEvent;
import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.cloudbalancer.dispatcher.scaling.AgentRegistry;
import com.cloudbalancer.dispatcher.scaling.PendingWorkerTracker;
import com.cloudbalancer.dispatcher.service.WorkerRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
public class AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentEventListener.class);

    private final AgentRegistry agentRegistry;
    private final PendingWorkerTracker pendingTracker;
    private final WorkerRegistryService workerRegistry;
    private final EventPublisher eventPublisher;

    public AgentEventListener(AgentRegistry agentRegistry,
                              PendingWorkerTracker pendingTracker,
                              WorkerRegistryService workerRegistry,
                              EventPublisher eventPublisher) {
        this.agentRegistry = agentRegistry;
        this.pendingTracker = pendingTracker;
        this.workerRegistry = workerRegistry;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "agents.heartbeat", groupId = "dispatcher-agents")
    public void onHeartbeat(String message) {
        try {
            var hb = JsonUtil.mapper().readValue(message, AgentHeartbeat.class);
            agentRegistry.updateAgent(hb);
        } catch (Exception e) {
            log.error("Failed to process agent heartbeat", e);
        }
    }

    @KafkaListener(topics = "agents.events", groupId = "dispatcher-agents")
    public void onAgentEvent(String message) {
        try {
            var event = JsonUtil.mapper().readValue(message, AgentEvent.class);

            switch (event) {
                case AgentEvent.WorkerStartedEvent started -> handleWorkerStarted(started);
                case AgentEvent.WorkerStartFailedEvent failed -> handleWorkerStartFailed(failed);
                case AgentEvent.WorkerStoppedEvent stopped -> handleWorkerStopped(stopped);
                case AgentEvent.WorkerStopFailedEvent stopFailed -> handleWorkerStopFailed(stopFailed);
            }
        } catch (Exception e) {
            log.error("Failed to process agent event", e);
        }
    }

    @KafkaListener(topics = "agents.registration", groupId = "dispatcher-agents")
    public void onAgentRegistration(String message) {
        try {
            var event = JsonUtil.mapper().readValue(message, AgentRegisteredEvent.class);
            log.info("Agent registered: {} ({})", event.agentId(), event.hostname());
        } catch (Exception e) {
            log.error("Failed to process agent registration", e);
        }
    }

    private void handleWorkerStarted(AgentEvent.WorkerStartedEvent event) {
        pendingTracker.resolve(event.workerId());

        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED, ExecutorType.SHELL, ExecutorType.DOCKER),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of());
        workerRegistry.registerWorker(event.workerId(), WorkerHealthState.HEALTHY, capabilities);

        eventPublisher.publishEvent("workers.registration", event.workerId(),
            new WorkerRegisteredEvent(UUID.randomUUID().toString(), Instant.now(),
                event.workerId(), capabilities));

        log.info("Worker {} started on agent {} (container: {})",
            event.workerId(), event.agentId(), event.containerId());
    }

    private void handleWorkerStartFailed(AgentEvent.WorkerStartFailedEvent event) {
        pendingTracker.fail(event.workerId());
        log.error("Worker {} failed to start on agent {}: {}",
            event.workerId(), event.agentId(), event.reason());
    }

    private void handleWorkerStopped(AgentEvent.WorkerStoppedEvent event) {
        workerRegistry.markDead(event.workerId());
        log.info("Worker {} stopped on agent {}", event.workerId(), event.agentId());
    }

    private void handleWorkerStopFailed(AgentEvent.WorkerStopFailedEvent event) {
        log.error("Worker {} failed to stop on agent {}: {}",
            event.workerId(), event.agentId(), event.reason());
    }
}

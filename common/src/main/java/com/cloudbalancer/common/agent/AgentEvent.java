package com.cloudbalancer.common.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentEvent.WorkerStartedEvent.class, name = "WORKER_STARTED"),
    @JsonSubTypes.Type(value = AgentEvent.WorkerStartFailedEvent.class, name = "WORKER_START_FAILED"),
    @JsonSubTypes.Type(value = AgentEvent.WorkerStoppedEvent.class, name = "WORKER_STOPPED"),
    @JsonSubTypes.Type(value = AgentEvent.WorkerStopFailedEvent.class, name = "WORKER_STOP_FAILED")
})
public sealed interface AgentEvent {
    String agentId();
    String workerId();

    record WorkerStartedEvent(String agentId, String workerId, String containerId, Instant timestamp) implements AgentEvent {}
    record WorkerStartFailedEvent(String agentId, String workerId, String reason, Instant timestamp) implements AgentEvent {}
    record WorkerStoppedEvent(String agentId, String workerId, Instant timestamp) implements AgentEvent {}
    record WorkerStopFailedEvent(String agentId, String workerId, String reason, Instant timestamp) implements AgentEvent {}
}

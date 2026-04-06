package com.cloudbalancer.common.agent;

import com.cloudbalancer.common.model.ExecutorType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentCommand.StartWorkerCommand.class, name = "START_WORKER"),
    @JsonSubTypes.Type(value = AgentCommand.StopWorkerCommand.class, name = "STOP_WORKER")
})
public sealed interface AgentCommand {
    String agentId();

    record StartWorkerCommand(
        String agentId,
        String workerId,
        int cpuCores,
        int memoryMB,
        int diskMB,
        Set<ExecutorType> supportedExecutors,
        Set<String> tags,
        Map<String, String> environment
    ) implements AgentCommand {}

    record StopWorkerCommand(
        String agentId,
        String workerId,
        boolean drain,
        int drainTimeSeconds
    ) implements AgentCommand {}
}

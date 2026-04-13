package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.ScalingAction;
import com.cloudbalancer.common.model.ScalingTriggerType;
import java.time.Instant;
import java.util.List;

public record ScalingEvent(
    String eventId,
    Instant timestamp,
    ScalingAction action,
    ScalingTriggerType triggerType,
    String reason,
    int previousWorkerCount,
    int newWorkerCount,
    List<String> workersAdded,
    List<String> workersRemoved,
    String agentId
) implements CloudBalancerEvent {
    @Override
    public String eventType() { return "SCALING_DECISION"; }
}

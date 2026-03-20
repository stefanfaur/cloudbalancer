package com.cloudbalancer.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TaskSubmittedEvent.class, name = "TASK_SUBMITTED"),
    @JsonSubTypes.Type(value = TaskStateChangedEvent.class, name = "TASK_STATE_CHANGED"),
    @JsonSubTypes.Type(value = TaskCompletedEvent.class, name = "TASK_COMPLETED"),
    @JsonSubTypes.Type(value = WorkerRegisteredEvent.class, name = "WORKER_REGISTERED"),
    @JsonSubTypes.Type(value = WorkerHeartbeatEvent.class, name = "WORKER_HEARTBEAT"),
    @JsonSubTypes.Type(value = WorkerMetricsEvent.class, name = "WORKER_METRICS"),
    @JsonSubTypes.Type(value = TaskAssignedEvent.class, name = "TASK_ASSIGNED")
})
public sealed interface CloudBalancerEvent
        permits TaskSubmittedEvent, TaskStateChangedEvent, TaskCompletedEvent,
                WorkerRegisteredEvent, WorkerHeartbeatEvent, WorkerMetricsEvent,
                TaskAssignedEvent {
    String eventId();
    Instant timestamp();
    String eventType();
}

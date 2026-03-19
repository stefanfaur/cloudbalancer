package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class EventSerializationTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void taskSubmittedEventRoundTrip() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        var event = new TaskSubmittedEvent(UUID.randomUUID().toString(), Instant.now(), UUID.randomUUID(), descriptor);

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"TASK_SUBMITTED\"");

        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(TaskSubmittedEvent.class);
        TaskSubmittedEvent typed = (TaskSubmittedEvent) deserialized;
        assertThat(typed.taskId()).isNotNull();
        assertThat(typed.descriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
    }

    @Test
    void taskStateChangedEventRoundTrip() throws Exception {
        var event = new TaskStateChangedEvent(
            UUID.randomUUID().toString(), Instant.now(),
            UUID.randomUUID(), TaskState.QUEUED, TaskState.ASSIGNED, "Worker selected"
        );

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"TASK_STATE_CHANGED\"");

        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(TaskStateChangedEvent.class);
        TaskStateChangedEvent typed = (TaskStateChangedEvent) deserialized;
        assertThat(typed.previousState()).isEqualTo(TaskState.QUEUED);
        assertThat(typed.newState()).isEqualTo(TaskState.ASSIGNED);
    }

    @Test
    void taskCompletedEventRoundTrip() throws Exception {
        var event = new TaskCompletedEvent(
            UUID.randomUUID().toString(), Instant.now(),
            UUID.randomUUID(), 0, "output data", ""
        );

        String json = mapper.writeValueAsString(event);
        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(TaskCompletedEvent.class);
    }

    @Test
    void workerRegisteredEventRoundTrip() throws Exception {
        var capabilities = new WorkerCapabilities(
            Set.of(ExecutorType.SIMULATED),
            new ResourceProfile(4, 8192, 10240, false, 0, true),
            Set.of()
        );
        var event = new WorkerRegisteredEvent(
            UUID.randomUUID().toString(), Instant.now(), "worker-1", capabilities
        );

        String json = mapper.writeValueAsString(event);
        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(WorkerRegisteredEvent.class);
        assertThat(((WorkerRegisteredEvent) deserialized).workerId()).isEqualTo("worker-1");
    }

    @Test
    void workerHeartbeatEventRoundTrip() throws Exception {
        var metrics = new WorkerMetrics(45.0, 60.0, 2, 0, 10.0, 200.0);
        var event = new WorkerHeartbeatEvent(
            UUID.randomUUID().toString(), Instant.now(), "worker-1", metrics
        );

        String json = mapper.writeValueAsString(event);
        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(WorkerHeartbeatEvent.class);
    }

    @Test
    void eventTypeDiscriminatorPresent() throws Exception {
        var event = new TaskStateChangedEvent(
            "evt-1", Instant.now(),
            UUID.randomUUID(), TaskState.SUBMITTED, TaskState.VALIDATED, null
        );
        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\"");
    }
}

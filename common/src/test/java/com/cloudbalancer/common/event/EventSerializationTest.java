package com.cloudbalancer.common.event;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
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
        var event = new WorkerHeartbeatEvent(
            UUID.randomUUID().toString(), Instant.now(), "worker-1", WorkerHealthState.HEALTHY
        );

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"WORKER_HEARTBEAT\"");
        assertThat(json).contains("\"workerId\"");
        assertThat(json).contains("\"healthState\"");

        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(WorkerHeartbeatEvent.class);
        WorkerHeartbeatEvent typed = (WorkerHeartbeatEvent) deserialized;
        assertThat(typed.workerId()).isEqualTo("worker-1");
        assertThat(typed.healthState()).isEqualTo(WorkerHealthState.HEALTHY);
    }

    @Test
    void scalingEventRoundTrip() throws Exception {
        var event = new ScalingEvent(
            UUID.randomUUID().toString(), Instant.now(),
            ScalingAction.SCALE_UP, ScalingTriggerType.REACTIVE,
            "CPU > 80% for 2 min", 3, 4,
            List.of("auto-local-4"), List.of()
        );

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"eventType\":\"SCALING_DECISION\"");

        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);
        assertThat(deserialized).isInstanceOf(ScalingEvent.class);
        ScalingEvent typed = (ScalingEvent) deserialized;
        assertThat(typed.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(typed.previousWorkerCount()).isEqualTo(3);
        assertThat(typed.newWorkerCount()).isEqualTo(4);
    }

    @Test
    void drainCommandRoundTrip() throws Exception {
        var cmd = new DrainCommand("worker-1", 60, Instant.now());
        String json = mapper.writeValueAsString(cmd);
        DrainCommand deserialized = mapper.readValue(json, DrainCommand.class);
        assertThat(deserialized.workerId()).isEqualTo("worker-1");
        assertThat(deserialized.drainTimeSeconds()).isEqualTo(60);
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

package com.cloudbalancer.common.model;

import com.cloudbalancer.common.event.CloudBalancerEvent;
import com.cloudbalancer.common.event.TaskAssignedEvent;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaDtoSerializationTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void taskAssignmentRoundTrip() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        var assignment = new TaskAssignment(
            UUID.randomUUID(), descriptor, "worker-1", Instant.now()
        );

        String json = mapper.writeValueAsString(assignment);
        TaskAssignment deserialized = mapper.readValue(json, TaskAssignment.class);

        assertThat(deserialized.taskId()).isEqualTo(assignment.taskId());
        assertThat(deserialized.assignedWorkerId()).isEqualTo("worker-1");
        assertThat(deserialized.descriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
    }

    @Test
    void taskResultRoundTrip() throws Exception {
        var result = new TaskResult(
            UUID.randomUUID(), "worker-1", 0, "hello", "", 1500, false, Instant.now()
        );

        String json = mapper.writeValueAsString(result);
        TaskResult deserialized = mapper.readValue(json, TaskResult.class);

        assertThat(deserialized.taskId()).isEqualTo(result.taskId());
        assertThat(deserialized.exitCode()).isEqualTo(0);
        assertThat(deserialized.stdout()).isEqualTo("hello");
        assertThat(deserialized.workerId()).isEqualTo("worker-1");
    }

    @Test
    void taskAssignedEventPolymorphicDeserialization() throws Exception {
        var event = new TaskAssignedEvent(
            UUID.randomUUID().toString(), Instant.now(),
            UUID.randomUUID(), "worker-1"
        );

        String json = mapper.writeValueAsString(event);
        CloudBalancerEvent deserialized = mapper.readValue(json, CloudBalancerEvent.class);

        assertThat(deserialized).isInstanceOf(TaskAssignedEvent.class);
        TaskAssignedEvent received = (TaskAssignedEvent) deserialized;
        assertThat(received.workerId()).isEqualTo("worker-1");
        assertThat(received.eventType()).isEqualTo("TASK_ASSIGNED");
    }
}

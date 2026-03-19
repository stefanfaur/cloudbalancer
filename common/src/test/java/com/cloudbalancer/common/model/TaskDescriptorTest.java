package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TaskDescriptorTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void fullDescriptorRoundTrip() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 5000, "failProbability", 0.1),
            new ResourceProfile(2, 4096, 1024, false, 60, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );

        String json = mapper.writeValueAsString(descriptor);
        TaskDescriptor deserialized = mapper.readValue(json, TaskDescriptor.class);

        assertThat(deserialized.executorType()).isEqualTo(ExecutorType.SIMULATED);
        assertThat(deserialized.executionSpec()).containsEntry("durationMs", 5000);
        assertThat(deserialized.priority()).isEqualTo(Priority.NORMAL);
        assertThat(deserialized.resourceProfile().cpuCores()).isEqualTo(2);
    }

    @Test
    void descriptorWithNullOptionalFields() throws Exception {
        var descriptor = new TaskDescriptor(
            ExecutorType.SHELL,
            Map.of("command", "echo hello"),
            new ResourceProfile(1, 512, 256, false, 10, false),
            null, // no constraints
            Priority.LOW,
            null, // default policy
            null  // no IO
        );

        String json = mapper.writeValueAsString(descriptor);
        assertThat(json).contains("\"executorType\":\"SHELL\"");

        TaskDescriptor deserialized = mapper.readValue(json, TaskDescriptor.class);
        assertThat(deserialized.executorType()).isEqualTo(ExecutorType.SHELL);
        assertThat(deserialized.constraints()).isNull();
    }

    @Test
    void executionAttemptRoundTrip() throws Exception {
        var attempt = new ExecutionAttempt(
            1,
            "worker-1",
            Instant.parse("2026-03-19T10:00:00Z"),
            Instant.parse("2026-03-19T10:01:00Z"),
            0,
            new ResourceProfile(2, 3500, 800, false, 60, false)
        );

        String json = mapper.writeValueAsString(attempt);
        ExecutionAttempt deserialized = mapper.readValue(json, ExecutionAttempt.class);

        assertThat(deserialized.attemptNumber()).isEqualTo(1);
        assertThat(deserialized.workerId()).isEqualTo("worker-1");
        assertThat(deserialized.exitCode()).isEqualTo(0);
        assertThat(deserialized.actualResources().memoryMB()).isEqualTo(3500);
    }
}

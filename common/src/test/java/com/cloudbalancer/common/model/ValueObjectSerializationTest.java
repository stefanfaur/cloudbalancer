package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ValueObjectSerializationTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @Test
    void resourceProfileRoundTrip() throws Exception {
        var profile = new ResourceProfile(4, 8192, 10240, true, 300, true);
        String json = mapper.writeValueAsString(profile);
        ResourceProfile deserialized = mapper.readValue(json, ResourceProfile.class);
        assertThat(deserialized).isEqualTo(profile);
    }

    @Test
    void resourceProfileDefaultsForOptionalFields() throws Exception {
        // GPU and network not required by default
        var profile = new ResourceProfile(2, 4096, 5120, false, 60, false);
        assertThat(profile.gpuRequired()).isFalse();
        assertThat(profile.networkAccessRequired()).isFalse();
    }

    @Test
    void executionPolicyRoundTrip() throws Exception {
        var policy = new ExecutionPolicy(3, 600, BackoffStrategy.EXPONENTIAL_WITH_JITTER, FailureAction.RETRY);
        String json = mapper.writeValueAsString(policy);
        ExecutionPolicy deserialized = mapper.readValue(json, ExecutionPolicy.class);
        assertThat(deserialized).isEqualTo(policy);
    }

    @Test
    void executionPolicyDefaults() throws Exception {
        var policy = ExecutionPolicy.defaults();
        assertThat(policy.maxRetries()).isEqualTo(3);
        assertThat(policy.retryBackoffStrategy()).isEqualTo(BackoffStrategy.EXPONENTIAL_WITH_JITTER);
        assertThat(policy.failureAction()).isEqualTo(FailureAction.RETRY);
    }

    @Test
    void taskConstraintsRoundTrip() throws Exception {
        var constraints = new TaskConstraints(
            Set.of("gpu-enabled", "region-eu"),
            Set.of("worker-5"),
            Set.of()
        );
        String json = mapper.writeValueAsString(constraints);
        TaskConstraints deserialized = mapper.readValue(json, TaskConstraints.class);
        assertThat(deserialized.requiredTags()).containsExactlyInAnyOrder("gpu-enabled", "region-eu");
        assertThat(deserialized.blacklistedWorkers()).containsExactly("worker-5");
    }

    @Test
    void taskConstraintsEmpty() throws Exception {
        var constraints = TaskConstraints.unconstrained();
        String json = mapper.writeValueAsString(constraints);
        TaskConstraints deserialized = mapper.readValue(json, TaskConstraints.class);
        assertThat(deserialized.requiredTags()).isEmpty();
    }

    @Test
    void taskIoRoundTrip() throws Exception {
        var io = new TaskIO(
            List.of(new TaskIO.InputArtifact("data.csv", "https://example.com/data.csv", TaskIO.ArtifactSource.HTTP)),
            List.of(new TaskIO.OutputArtifact("result.json", "results/"))
        );
        String json = mapper.writeValueAsString(io);
        TaskIO deserialized = mapper.readValue(json, TaskIO.class);
        assertThat(deserialized.inputs()).hasSize(1);
        assertThat(deserialized.inputs().getFirst().name()).isEqualTo("data.csv");
        assertThat(deserialized.outputs()).hasSize(1);
    }
}

package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

class EnumSerializationTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    @ParameterizedTest
    @EnumSource(Priority.class)
    void priorityRoundTrip(Priority value) throws Exception {
        String json = mapper.writeValueAsString(value);
        Priority deserialized = mapper.readValue(json, Priority.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(TaskState.class)
    void taskStateRoundTrip(TaskState value) throws Exception {
        String json = mapper.writeValueAsString(value);
        TaskState deserialized = mapper.readValue(json, TaskState.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(ExecutorType.class)
    void executorTypeRoundTrip(ExecutorType value) throws Exception {
        String json = mapper.writeValueAsString(value);
        ExecutorType deserialized = mapper.readValue(json, ExecutorType.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(WorkerHealthState.class)
    void workerHealthStateRoundTrip(WorkerHealthState value) throws Exception {
        String json = mapper.writeValueAsString(value);
        WorkerHealthState deserialized = mapper.readValue(json, WorkerHealthState.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(BackoffStrategy.class)
    void backoffStrategyRoundTrip(BackoffStrategy value) throws Exception {
        String json = mapper.writeValueAsString(value);
        BackoffStrategy deserialized = mapper.readValue(json, BackoffStrategy.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(FailureAction.class)
    void failureActionRoundTrip(FailureAction value) throws Exception {
        String json = mapper.writeValueAsString(value);
        FailureAction deserialized = mapper.readValue(json, FailureAction.class);
        assertThat(deserialized).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(SecurityLevel.class)
    void securityLevelRoundTrip(SecurityLevel value) throws Exception {
        String json = mapper.writeValueAsString(value);
        SecurityLevel deserialized = mapper.readValue(json, SecurityLevel.class);
        assertThat(deserialized).isEqualTo(value);
    }
}

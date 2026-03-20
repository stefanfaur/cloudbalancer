package com.cloudbalancer.worker.service;

import com.cloudbalancer.common.model.*;
import com.cloudbalancer.common.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void executesTaskAndPublishesSuccessResult() throws Exception {
        var service = new TaskExecutionService(kafkaTemplate, "test-worker");
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100, "failProbability", 0.0),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        var assignment = new TaskAssignment(UUID.randomUUID(), descriptor, "test-worker", Instant.now());

        service.executeTask(assignment);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("tasks.results"), eq(assignment.taskId().toString()), valueCaptor.capture());

        TaskResult result = JsonUtil.mapper().readValue(valueCaptor.getValue(), TaskResult.class);
        assertThat(result.taskId()).isEqualTo(assignment.taskId());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.workerId()).isEqualTo("test-worker");
    }

    @Test
    void executesTaskAndPublishesFailureResult() throws Exception {
        var service = new TaskExecutionService(kafkaTemplate, "test-worker");
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 100, "failProbability", 1.0),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            ExecutionPolicy.defaults(), TaskIO.none()
        );
        var assignment = new TaskAssignment(UUID.randomUUID(), descriptor, "test-worker", Instant.now());

        service.executeTask(assignment);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("tasks.results"), eq(assignment.taskId().toString()), valueCaptor.capture());

        TaskResult result = JsonUtil.mapper().readValue(valueCaptor.getValue(), TaskResult.class);
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void respectsExecutionTimeout() throws Exception {
        var service = new TaskExecutionService(kafkaTemplate, "test-worker");
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED, Map.of("durationMs", 30000, "failProbability", 0.0),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(), Priority.NORMAL,
            new ExecutionPolicy(3, 1, BackoffStrategy.FIXED, FailureAction.RETRY), // 1 second timeout
            TaskIO.none()
        );
        var assignment = new TaskAssignment(UUID.randomUUID(), descriptor, "test-worker", Instant.now());

        long start = System.currentTimeMillis();
        service.executeTask(assignment);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(5000); // should not run for 30 seconds

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("tasks.results"), eq(assignment.taskId().toString()), valueCaptor.capture());

        TaskResult result = JsonUtil.mapper().readValue(valueCaptor.getValue(), TaskResult.class);
        assertThat(result.timedOut()).isTrue();
        assertThat(result.succeeded()).isFalse();
    }
}

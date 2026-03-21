package com.cloudbalancer.common.model;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class TaskEnvelopeStateMachineTest {

    private final ObjectMapper mapper = JsonUtil.mapper();

    private TaskEnvelope createTestEnvelope() {
        var descriptor = new TaskDescriptor(
            ExecutorType.SIMULATED,
            Map.of("durationMs", 1000),
            new ResourceProfile(1, 512, 256, false, 10, false),
            TaskConstraints.unconstrained(),
            Priority.NORMAL,
            ExecutionPolicy.defaults(),
            TaskIO.none()
        );
        return TaskEnvelope.create(descriptor);
    }

    @Test
    void newEnvelopeStartsInSubmittedState() {
        TaskEnvelope envelope = createTestEnvelope();
        assertThat(envelope.getState()).isEqualTo(TaskState.SUBMITTED);
        assertThat(envelope.getId()).isNotNull();
        assertThat(envelope.getSubmittedAt()).isNotNull();
        assertThat(envelope.getExecutionHistory()).isEmpty();
    }

    // --- Valid transitions ---

    @Test
    void submittedToValidated() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        assertThat(envelope.getState()).isEqualTo(TaskState.VALIDATED);
    }

    @Test
    void submittedToFailed() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.FAILED);
        assertThat(envelope.getState()).isEqualTo(TaskState.FAILED);
    }

    @Test
    void submittedToCancelled() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.CANCELLED);
        assertThat(envelope.getState()).isEqualTo(TaskState.CANCELLED);
    }

    @Test
    void fullHappyPath() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.POST_PROCESSING);
        envelope.transitionTo(TaskState.COMPLETED);
        assertThat(envelope.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void runningToTimedOut() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.TIMED_OUT);
        assertThat(envelope.getState()).isEqualTo(TaskState.TIMED_OUT);
    }

    @Test
    void runningToFailed() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.FAILED);
        assertThat(envelope.getState()).isEqualTo(TaskState.FAILED);
    }

    @Test
    void cancelFromQueued() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.CANCELLED);
        assertThat(envelope.getState()).isEqualTo(TaskState.CANCELLED);
    }

    @Test
    void cancelFromRunning() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.CANCELLED);
        assertThat(envelope.getState()).isEqualTo(TaskState.CANCELLED);
    }

    @Test
    void failedToQueuedForRetry() {
        // Retry transitions: FAILED -> QUEUED (Phase 6 retry engine triggers this)
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.FAILED);
        // Retry: go back to QUEUED
        envelope.transitionTo(TaskState.QUEUED);
        assertThat(envelope.getState()).isEqualTo(TaskState.QUEUED);
    }

    // --- Invalid transitions ---

    @Test
    void submittedCannotGoDirectlyToRunning() {
        TaskEnvelope envelope = createTestEnvelope();
        assertThatThrownBy(() -> envelope.transitionTo(TaskState.RUNNING))
            .isInstanceOf(IllegalStateTransitionException.class)
            .hasMessageContaining("SUBMITTED")
            .hasMessageContaining("RUNNING");
    }

    @Test
    void completedCannotTransitionAnywhere() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);
        envelope.transitionTo(TaskState.PROVISIONING);
        envelope.transitionTo(TaskState.RUNNING);
        envelope.transitionTo(TaskState.POST_PROCESSING);
        envelope.transitionTo(TaskState.COMPLETED);

        for (TaskState target : TaskState.values()) {
            assertThatThrownBy(() -> envelope.transitionTo(target))
                .isInstanceOf(IllegalStateTransitionException.class);
        }
    }

    @Test
    void queuedCannotGoBackToSubmitted() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        assertThatThrownBy(() -> envelope.transitionTo(TaskState.SUBMITTED))
            .isInstanceOf(IllegalStateTransitionException.class);
    }

    // --- Terminal state detection ---

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED", "DEAD_LETTERED"})
    void terminalStatesAreTerminal(TaskState state) {
        assertThat(state.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"SUBMITTED", "VALIDATED", "QUEUED", "ASSIGNED", "PROVISIONING", "RUNNING", "POST_PROCESSING"})
    void nonTerminalStatesAreNotTerminal(TaskState state) {
        assertThat(state.isTerminal()).isFalse();
    }

    // --- Serialization ---

    @Test
    void envelopeSerializationRoundTrip() throws Exception {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);

        String json = mapper.writeValueAsString(envelope);
        TaskEnvelope deserialized = mapper.readValue(json, TaskEnvelope.class);

        assertThat(deserialized.getId()).isEqualTo(envelope.getId());
        assertThat(deserialized.getState()).isEqualTo(TaskState.QUEUED);
        assertThat(deserialized.getDescriptor().executorType()).isEqualTo(ExecutorType.SIMULATED);
    }

    // --- Execution history ---

    @Test
    void addAttemptTracksHistory() {
        TaskEnvelope envelope = createTestEnvelope();
        envelope.transitionTo(TaskState.VALIDATED);
        envelope.transitionTo(TaskState.QUEUED);
        envelope.transitionTo(TaskState.ASSIGNED);

        var attempt = new ExecutionAttempt(1, "worker-1", java.time.Instant.now(), null, -1, null, null, false, null);
        envelope.addAttempt(attempt);

        assertThat(envelope.getExecutionHistory()).hasSize(1);
        assertThat(envelope.getExecutionHistory().getFirst().workerId()).isEqualTo("worker-1");
    }
}

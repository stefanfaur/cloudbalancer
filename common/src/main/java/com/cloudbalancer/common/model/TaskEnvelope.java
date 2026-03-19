package com.cloudbalancer.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TaskEnvelope {

    private final UUID id;
    private final TaskDescriptor descriptor;
    private final Instant submittedAt;
    private TaskState state;
    private final List<ExecutionAttempt> executionHistory;

    private TaskEnvelope(UUID id, TaskDescriptor descriptor, Instant submittedAt,
                         TaskState state, List<ExecutionAttempt> executionHistory) {
        this.id = id;
        this.descriptor = descriptor;
        this.submittedAt = submittedAt;
        this.state = state;
        this.executionHistory = new ArrayList<>(executionHistory);
    }

    public static TaskEnvelope create(TaskDescriptor descriptor) {
        return new TaskEnvelope(
            UUID.randomUUID(),
            descriptor,
            Instant.now(),
            TaskState.SUBMITTED,
            List.of()
        );
    }

    @JsonCreator
    public static TaskEnvelope fromJson(
            @JsonProperty("id") UUID id,
            @JsonProperty("descriptor") TaskDescriptor descriptor,
            @JsonProperty("submittedAt") Instant submittedAt,
            @JsonProperty("state") TaskState state,
            @JsonProperty("executionHistory") List<ExecutionAttempt> executionHistory) {
        return new TaskEnvelope(id, descriptor, submittedAt, state,
            executionHistory != null ? executionHistory : List.of());
    }

    public void transitionTo(TaskState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateTransitionException(state, newState);
        }
        this.state = newState;
    }

    public void addAttempt(ExecutionAttempt attempt) {
        this.executionHistory.add(attempt);
    }

    public UUID getId() { return id; }
    public TaskDescriptor getDescriptor() { return descriptor; }
    public Instant getSubmittedAt() { return submittedAt; }
    public TaskState getState() { return state; }
    public List<ExecutionAttempt> getExecutionHistory() {
        return Collections.unmodifiableList(executionHistory);
    }
}

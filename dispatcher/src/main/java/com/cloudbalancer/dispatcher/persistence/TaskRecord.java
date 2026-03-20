package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "executor_type", nullable = false)
    private ExecutorType executorType;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "assigned_worker_id")
    private String assignedWorkerId;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = TaskDescriptorConverter.class)
    private TaskDescriptor descriptor;

    @Column(name = "execution_history", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = ExecutionHistoryConverter.class)
    private List<ExecutionAttempt> executionHistory = new ArrayList<>();

    protected TaskRecord() {}

    public static TaskRecord create(TaskDescriptor descriptor) {
        var record = new TaskRecord();
        record.id = UUID.randomUUID();
        record.state = TaskState.SUBMITTED;
        record.priority = descriptor.priority();
        record.executorType = descriptor.executorType();
        record.submittedAt = Instant.now();
        record.descriptor = descriptor;
        record.executionHistory = new ArrayList<>();
        return record;
    }

    public void transitionTo(TaskState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition from " + state + " to " + newState);
        }
        this.state = newState;
    }

    public UUID getId() { return id; }
    public TaskState getState() { return state; }
    public Priority getPriority() { return priority; }
    public ExecutorType getExecutorType() { return executorType; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getAssignedWorkerId() { return assignedWorkerId; }
    public void setAssignedWorkerId(String assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }
    public TaskDescriptor getDescriptor() { return descriptor; }
    public List<ExecutionAttempt> getExecutionHistory() { return executionHistory; }

    public void addAttempt(ExecutionAttempt attempt) {
        this.executionHistory.add(attempt);
    }

    public TaskEnvelope toEnvelope() {
        return TaskEnvelope.fromJson(id, descriptor, submittedAt, state, executionHistory);
    }
}

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

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "retry_eligible_at")
    private Instant retryEligibleAt;

    @Column(name = "current_execution_id", columnDefinition = "uuid")
    private UUID currentExecutionId;

    @Column(name = "last_stdout", columnDefinition = "TEXT")
    private String lastStdout;

    @Column(name = "last_stderr", columnDefinition = "TEXT")
    private String lastStderr;

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
        record.currentExecutionId = UUID.randomUUID();
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
    public void setPriority(Priority priority) { this.priority = priority; }
    public ExecutorType getExecutorType() { return executorType; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getAssignedWorkerId() { return assignedWorkerId; }
    public void setAssignedWorkerId(String assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }
    public TaskDescriptor getDescriptor() { return descriptor; }
    public List<ExecutionAttempt> getExecutionHistory() { return executionHistory; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getRetryEligibleAt() { return retryEligibleAt; }
    public void setRetryEligibleAt(Instant retryEligibleAt) { this.retryEligibleAt = retryEligibleAt; }
    public UUID getCurrentExecutionId() { return currentExecutionId; }
    public void setCurrentExecutionId(UUID currentExecutionId) { this.currentExecutionId = currentExecutionId; }
    public String getLastStdout() { return lastStdout; }
    public void setLastStdout(String lastStdout) { this.lastStdout = lastStdout; }
    public String getLastStderr() { return lastStderr; }
    public void setLastStderr(String lastStderr) { this.lastStderr = lastStderr; }

    public void addAttempt(ExecutionAttempt attempt) {
        this.executionHistory.add(attempt);
    }

    public TaskEnvelope toEnvelope() {
        return TaskEnvelope.fromJson(id, descriptor, submittedAt, state, executionHistory);
    }
}

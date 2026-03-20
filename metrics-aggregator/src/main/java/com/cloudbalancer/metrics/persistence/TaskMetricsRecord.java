package com.cloudbalancer.metrics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_metrics", schema = "metrics")
public class TaskMetricsRecord {

    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "queue_wait_ms")
    private Long queueWaitMs;

    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;

    @Column(name = "turnaround_ms")
    private Long turnaroundMs;

    public TaskMetricsRecord() {}

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getQueueWaitMs() { return queueWaitMs; }
    public void setQueueWaitMs(Long queueWaitMs) { this.queueWaitMs = queueWaitMs; }

    public Long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(Long executionDurationMs) { this.executionDurationMs = executionDurationMs; }

    public Long getTurnaroundMs() { return turnaroundMs; }
    public void setTurnaroundMs(Long turnaroundMs) { this.turnaroundMs = turnaroundMs; }
}

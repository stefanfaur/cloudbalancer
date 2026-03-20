package com.cloudbalancer.metrics.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "worker_metrics", schema = "metrics")
public class WorkerMetricsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "cpu_usage_percent", nullable = false)
    private double cpuUsagePercent;

    @Column(name = "heap_used_mb", nullable = false)
    private long heapUsedMB;

    @Column(name = "heap_max_mb", nullable = false)
    private long heapMaxMB;

    @Column(name = "thread_count", nullable = false)
    private int threadCount;

    @Column(name = "active_task_count", nullable = false)
    private int activeTaskCount;

    @Column(name = "completed_task_count", nullable = false)
    private long completedTaskCount;

    @Column(name = "failed_task_count", nullable = false)
    private long failedTaskCount;

    @Column(name = "avg_execution_duration_ms", nullable = false)
    private double avgExecutionDurationMs;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    public WorkerMetricsRecord() {}

    public Long getId() { return id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

    public long getHeapUsedMB() { return heapUsedMB; }
    public void setHeapUsedMB(long heapUsedMB) { this.heapUsedMB = heapUsedMB; }

    public long getHeapMaxMB() { return heapMaxMB; }
    public void setHeapMaxMB(long heapMaxMB) { this.heapMaxMB = heapMaxMB; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getActiveTaskCount() { return activeTaskCount; }
    public void setActiveTaskCount(int activeTaskCount) { this.activeTaskCount = activeTaskCount; }

    public long getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(long completedTaskCount) { this.completedTaskCount = completedTaskCount; }

    public long getFailedTaskCount() { return failedTaskCount; }
    public void setFailedTaskCount(long failedTaskCount) { this.failedTaskCount = failedTaskCount; }

    public double getAvgExecutionDurationMs() { return avgExecutionDurationMs; }
    public void setAvgExecutionDurationMs(double avgExecutionDurationMs) { this.avgExecutionDurationMs = avgExecutionDurationMs; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}

package com.cloudbalancer.metrics.service;

import com.cloudbalancer.metrics.api.dto.ClusterMetrics;
import com.cloudbalancer.metrics.api.dto.WorkerMetricsBucket;
import com.cloudbalancer.metrics.api.dto.WorkerMetricsSnapshot;
import com.cloudbalancer.metrics.persistence.TaskMetricsRecord;
import com.cloudbalancer.metrics.persistence.TaskMetricsRepository;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRecord;
import com.cloudbalancer.metrics.persistence.WorkerHeartbeatRepository;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRecord;
import com.cloudbalancer.metrics.persistence.WorkerMetricsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MetricsAggregationService {

    private final WorkerMetricsRepository metricsRepo;
    private final WorkerHeartbeatRepository heartbeatRepo;
    private final TaskMetricsRepository taskMetricsRepo;
    private final EntityManager entityManager;

    public MetricsAggregationService(WorkerMetricsRepository metricsRepo,
                                     WorkerHeartbeatRepository heartbeatRepo,
                                     TaskMetricsRepository taskMetricsRepo,
                                     EntityManager entityManager) {
        this.metricsRepo = metricsRepo;
        this.heartbeatRepo = heartbeatRepo;
        this.taskMetricsRepo = taskMetricsRepo;
        this.entityManager = entityManager;
    }

    /**
     * Returns the latest metrics snapshot for each worker.
     */
    public List<WorkerMetricsSnapshot> getLatestPerWorker() {
        return metricsRepo.findLatestPerWorker().stream()
                .map(this::toSnapshot)
                .collect(Collectors.toList());
    }

    /**
     * Returns worker history, optionally bucketed using TimescaleDB time_bucket().
     *
     * @param workerId      the worker ID
     * @param from          start of time range
     * @param to            end of time range
     * @param bucketMinutes bucket size in minutes (0 or 1 = raw rows)
     */
    public List<WorkerMetricsBucket> getWorkerHistory(String workerId, Instant from, Instant to,
                                                       int bucketMinutes) {
        if (bucketMinutes <= 1) {
            // Return raw rows as single-row "buckets"
            List<WorkerMetricsRecord> rows = metricsRepo.findByWorkerIdAndTimeRange(workerId, from, to);
            return rows.stream()
                    .map(r -> new WorkerMetricsBucket(
                            r.getReportedAt(), r.getWorkerId(),
                            r.getCpuUsagePercent(), r.getHeapUsedMB(), r.getHeapMaxMB(),
                            r.getThreadCount(), r.getActiveTaskCount(),
                            r.getCompletedTaskCount(), r.getFailedTaskCount(),
                            r.getAvgExecutionDurationMs()))
                    .collect(Collectors.toList());
        }

        // Use TimescaleDB time_bucket for aggregation
        String sql = "SELECT time_bucket(:interval, reported_at) AS bucket, " +
                "worker_id, " +
                "AVG(cpu_usage_percent) AS avg_cpu, " +
                "AVG(heap_used_mb) AS avg_heap_used, " +
                "AVG(heap_max_mb) AS avg_heap_max, " +
                "AVG(thread_count) AS avg_threads, " +
                "AVG(active_task_count) AS avg_active_tasks, " +
                "AVG(completed_task_count) AS avg_completed, " +
                "AVG(failed_task_count) AS avg_failed, " +
                "AVG(avg_execution_duration_ms) AS avg_exec_duration " +
                "FROM metrics.worker_metrics " +
                "WHERE worker_id = :workerId AND reported_at >= :from AND reported_at <= :to " +
                "GROUP BY bucket, worker_id ORDER BY bucket";

        String interval = bucketMinutes + " minutes";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("interval", interval)
                .setParameter("workerId", workerId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        return results.stream()
                .map(row -> new WorkerMetricsBucket(
                        toInstant(row[0]),
                        (String) row[1],
                        toDouble(row[2]),
                        toLong(row[3]),
                        toLong(row[4]),
                        toInt(row[5]),
                        toInt(row[6]),
                        toLong(row[7]),
                        toLong(row[8]),
                        toDouble(row[9])))
                .collect(Collectors.toList());
    }

    /**
     * Computes cluster-wide aggregate metrics.
     */
    public ClusterMetrics getClusterMetrics() {
        List<WorkerMetricsRecord> latestPerWorker = metricsRepo.findLatestPerWorker();
        List<WorkerHeartbeatRecord> latestHeartbeats = heartbeatRepo.findLatestPerWorker();

        int workerCount = latestPerWorker.size();
        if (workerCount == 0) {
            return new ClusterMetrics(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double avgCpu = latestPerWorker.stream()
                .mapToDouble(WorkerMetricsRecord::getCpuUsagePercent)
                .average().orElse(0);
        int totalActiveTasks = latestPerWorker.stream()
                .mapToInt(WorkerMetricsRecord::getActiveTaskCount)
                .sum();
        long totalHeapUsed = latestPerWorker.stream()
                .mapToLong(WorkerMetricsRecord::getHeapUsedMB)
                .sum();

        int healthyCount = (int) latestHeartbeats.stream()
                .filter(h -> "HEALTHY".equals(h.getHealthState()))
                .count();

        // Task throughput and averages from last minute
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        List<TaskMetricsRecord> recentTasks = taskMetricsRepo.findCompletedSince(oneMinuteAgo);
        double throughput = recentTasks.size();

        double avgQueueWait = recentTasks.stream()
                .filter(t -> t.getQueueWaitMs() != null)
                .mapToLong(TaskMetricsRecord::getQueueWaitMs)
                .average().orElse(0);
        double avgExecDuration = recentTasks.stream()
                .filter(t -> t.getExecutionDurationMs() != null)
                .mapToLong(TaskMetricsRecord::getExecutionDurationMs)
                .average().orElse(0);

        return new ClusterMetrics(
                avgCpu, totalActiveTasks, totalHeapUsed,
                throughput, avgQueueWait, avgExecDuration,
                workerCount, healthyCount);
    }

    // ---- Mapping helpers ----

    private WorkerMetricsSnapshot toSnapshot(WorkerMetricsRecord r) {
        return new WorkerMetricsSnapshot(
                r.getWorkerId(), r.getCpuUsagePercent(), r.getHeapUsedMB(), r.getHeapMaxMB(),
                r.getThreadCount(), r.getActiveTaskCount(), r.getCompletedTaskCount(),
                r.getFailedTaskCount(), r.getAvgExecutionDurationMs(), r.getReportedAt());
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        throw new IllegalArgumentException("Cannot convert to Instant: " + value.getClass());
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }
}

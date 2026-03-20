package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkerMetricsRepository extends JpaRepository<WorkerMetricsRecord, Long> {

    /**
     * Returns the latest metrics row per worker, using PostgreSQL DISTINCT ON.
     */
    @Query(value = "SELECT DISTINCT ON (worker_id) * FROM metrics.worker_metrics " +
            "ORDER BY worker_id, reported_at DESC", nativeQuery = true)
    List<WorkerMetricsRecord> findLatestPerWorker();

    /**
     * Returns raw metrics rows for a specific worker within a time range.
     */
    @Query(value = "SELECT * FROM metrics.worker_metrics " +
            "WHERE worker_id = :workerId AND reported_at >= :from AND reported_at <= :to " +
            "ORDER BY reported_at", nativeQuery = true)
    List<WorkerMetricsRecord> findByWorkerIdAndTimeRange(
            @Param("workerId") String workerId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}

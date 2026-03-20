package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskMetricsRepository extends JpaRepository<TaskMetricsRecord, UUID> {

    /**
     * Count tasks completed since a given instant (for throughput calculation).
     */
    @Query("SELECT COUNT(t) FROM TaskMetricsRecord t WHERE t.completedAt >= :since")
    long countCompletedSince(@Param("since") Instant since);

    /**
     * Find tasks completed since a given instant (for average calculations).
     */
    @Query("SELECT t FROM TaskMetricsRecord t WHERE t.completedAt >= :since")
    List<TaskMetricsRecord> findCompletedSince(@Param("since") Instant since);
}

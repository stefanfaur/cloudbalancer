package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeatRecord, Long> {

    /**
     * Returns the latest heartbeat per worker, using PostgreSQL DISTINCT ON.
     */
    @Query(value = "SELECT DISTINCT ON (worker_id) * FROM metrics.worker_heartbeats " +
            "ORDER BY worker_id, timestamp DESC", nativeQuery = true)
    List<WorkerHeartbeatRecord> findLatestPerWorker();
}

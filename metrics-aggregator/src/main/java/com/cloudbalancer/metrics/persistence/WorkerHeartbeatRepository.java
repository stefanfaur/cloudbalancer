package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeatRecord, Long> {
}

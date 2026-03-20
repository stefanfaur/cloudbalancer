package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerMetricsRepository extends JpaRepository<WorkerMetricsRecord, Long> {
}

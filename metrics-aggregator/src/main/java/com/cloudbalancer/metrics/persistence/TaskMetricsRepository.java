package com.cloudbalancer.metrics.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskMetricsRepository extends JpaRepository<TaskMetricsRecord, UUID> {
}

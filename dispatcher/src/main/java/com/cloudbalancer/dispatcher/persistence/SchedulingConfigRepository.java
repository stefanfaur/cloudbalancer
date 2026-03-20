package com.cloudbalancer.dispatcher.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulingConfigRepository extends JpaRepository<SchedulingConfigRecord, Long> {
}

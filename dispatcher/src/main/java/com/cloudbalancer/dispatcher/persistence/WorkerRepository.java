package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.WorkerHealthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerRepository extends JpaRepository<WorkerRecord, String> {

    List<WorkerRecord> findByHealthState(WorkerHealthState state);
}

package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.TaskState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskRecord, UUID> {

    List<TaskRecord> findByState(TaskState state);

    List<TaskRecord> findByAssignedWorkerId(String workerId);

    List<TaskRecord> findByStateIn(Collection<TaskState> states);

    List<TaskRecord> findByAssignedWorkerIdAndStateIn(String workerId, Collection<TaskState> states);

    long countByState(TaskState state);
}

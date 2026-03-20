package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SchedulingStrategy {
    Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates,
                                   Map<String, WorkerScorer> scorers);
    String getName();
    Map<String, Integer> getWeights();
}

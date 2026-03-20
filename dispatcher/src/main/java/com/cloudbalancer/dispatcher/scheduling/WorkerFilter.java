package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;

public interface WorkerFilter {
    List<WorkerRecord> filter(TaskRecord task, List<WorkerRecord> candidates);
}

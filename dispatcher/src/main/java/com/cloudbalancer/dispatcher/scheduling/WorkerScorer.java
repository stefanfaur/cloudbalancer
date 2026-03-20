package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

public interface WorkerScorer {
    int score(TaskRecord task, WorkerRecord worker); // 0-100
    String getScorerName();
}

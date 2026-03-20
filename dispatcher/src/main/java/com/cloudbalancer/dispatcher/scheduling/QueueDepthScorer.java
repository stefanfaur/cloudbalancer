package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

public class QueueDepthScorer implements WorkerScorer {

    private static final int MAX_TASKS_FOR_ZERO_SCORE = 100;

    @Override
    public int score(TaskRecord task, WorkerRecord worker) {
        int activeTasks = worker.getActiveTaskCount();
        if (activeTasks <= 0) return 100;
        if (activeTasks >= MAX_TASKS_FOR_ZERO_SCORE) return 0;
        // Linear decay: 100 at 0 tasks, 0 at MAX_TASKS_FOR_ZERO_SCORE
        return (int) Math.round(100.0 * (1.0 - (double) activeTasks / MAX_TASKS_FOR_ZERO_SCORE));
    }

    @Override
    public String getScorerName() {
        return "queueDepth";
    }
}

package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements SchedulingStrategy {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates,
                                          Map<String, WorkerScorer> scorers) {
        if (candidates.isEmpty()) return Optional.empty();
        int idx = Math.floorMod(index.getAndIncrement(), candidates.size());
        return Optional.of(candidates.get(idx));
    }

    @Override
    public String getName() {
        return "ROUND_ROBIN";
    }

    @Override
    public Map<String, Integer> getWeights() {
        return Map.of();
    }
}

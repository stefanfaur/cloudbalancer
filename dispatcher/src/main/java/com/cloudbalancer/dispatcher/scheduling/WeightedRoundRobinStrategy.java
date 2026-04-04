package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.common.model.ResourceProfile;
import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Capacity-proportional round-robin: workers with more total resources
 * get proportionally more assignments.
 */
public class WeightedRoundRobinStrategy implements SchedulingStrategy {

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates,
                                          Map<String, WorkerScorer> scorers) {
        if (candidates.isEmpty()) return Optional.empty();

        // Compute capacity weight for each worker (sum of total resources as a simple proxy)
        int[] cumulativeWeights = new int[candidates.size()];
        int totalWeight = 0;
        for (int i = 0; i < candidates.size(); i++) {
            ResourceProfile total = candidates.get(i).getCapabilities().totalResources();
            int weight = (total != null)
                ? Math.max(1, total.cpuCores() + total.memoryMB() / 256 + total.diskMB() / 256)
                : 1;
            totalWeight += weight;
            cumulativeWeights[i] = totalWeight;
        }

        long tick = Math.floorMod(counter.getAndIncrement(), totalWeight);
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (tick < cumulativeWeights[i]) {
                return Optional.of(candidates.get(i));
            }
        }

        return Optional.of(candidates.getLast());
    }

    @Override
    public String getName() {
        return "WEIGHTED_ROUND_ROBIN";
    }

    @Override
    public Map<String, Integer> getWeights() {
        return Map.of();
    }
}

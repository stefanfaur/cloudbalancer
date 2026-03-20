package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class WeightedScoringStrategy implements SchedulingStrategy {

    private final String name;
    private final Map<String, Integer> weights;

    protected WeightedScoringStrategy(String name, Map<String, Integer> weights) {
        this.name = name;
        this.weights = Map.copyOf(weights);
    }

    @Override
    public Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates,
                                          Map<String, WorkerScorer> scorers) {
        if (candidates.isEmpty()) return Optional.empty();

        return candidates.stream()
            .max(Comparator.comparingDouble(worker -> computeWeightedScore(task, worker, scorers)));
    }

    private double computeWeightedScore(TaskRecord task, WorkerRecord worker,
                                         Map<String, WorkerScorer> scorers) {
        double totalScore = 0;
        double totalWeight = 0;

        for (var entry : weights.entrySet()) {
            WorkerScorer scorer = scorers.get(entry.getKey());
            if (scorer != null && entry.getValue() > 0) {
                totalScore += scorer.score(task, worker) * entry.getValue();
                totalWeight += entry.getValue();
            }
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Integer> getWeights() {
        return weights;
    }
}

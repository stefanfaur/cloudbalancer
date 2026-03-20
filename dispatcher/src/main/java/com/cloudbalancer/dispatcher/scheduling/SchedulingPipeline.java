package com.cloudbalancer.dispatcher.scheduling;

import com.cloudbalancer.dispatcher.persistence.TaskRecord;
import com.cloudbalancer.dispatcher.persistence.WorkerRecord;
import com.cloudbalancer.dispatcher.service.SchedulingConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SchedulingPipeline {

    private static final Logger log = LoggerFactory.getLogger(SchedulingPipeline.class);

    private final List<WorkerFilter> filters;
    private final Map<String, WorkerScorer> scorers;
    private final SchedulingConfigService configService;

    public SchedulingPipeline(List<WorkerFilter> filters, List<WorkerScorer> scorerList,
                               SchedulingConfigService configService) {
        this.filters = filters;
        this.scorers = scorerList.stream()
            .collect(Collectors.toMap(WorkerScorer::getScorerName, s -> s));
        this.configService = configService;
    }

    public Optional<WorkerRecord> select(TaskRecord task, List<WorkerRecord> candidates) {
        // Stage 1: Filter
        List<WorkerRecord> filtered = new ArrayList<>(candidates);
        for (WorkerFilter filter : filters) {
            filtered = filter.filter(task, filtered);
            if (filtered.isEmpty()) {
                log.debug("Task {} eliminated all candidates at filter {}", task.getId(),
                    filter.getClass().getSimpleName());
                return Optional.empty();
            }
        }

        // Stage 2+3: Score and Select via current strategy
        SchedulingStrategy strategy = configService.getCurrentStrategy();
        var selected = strategy.select(task, filtered, scorers);
        selected.ifPresent(w -> log.debug("Task {} selected worker {} via strategy {}",
            task.getId(), w.getId(), strategy.getName()));
        return selected;
    }
}

package com.cloudbalancer.dispatcher.service;

import com.cloudbalancer.dispatcher.persistence.SchedulingConfigRecord;
import com.cloudbalancer.dispatcher.persistence.SchedulingConfigRepository;
import com.cloudbalancer.dispatcher.scheduling.SchedulingStrategy;
import com.cloudbalancer.dispatcher.scheduling.SchedulingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SchedulingConfigService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfigService.class);

    private final SchedulingConfigRepository configRepository;
    private volatile SchedulingStrategy currentStrategy;

    public SchedulingConfigService(SchedulingConfigRepository configRepository) {
        this.configRepository = configRepository;
        this.currentStrategy = loadOrDefault();
    }

    public SchedulingStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    public void switchStrategy(String name, Map<String, Integer> weights) {
        var config = configRepository.findAll().stream().findFirst().orElse(null);
        if (config == null) {
            config = new SchedulingConfigRecord(name, weights);
        } else {
            config.setStrategyName(name);
            config.setWeights(weights);
        }
        configRepository.save(config);
        this.currentStrategy = SchedulingStrategyFactory.create(name, weights);
        log.info("Scheduling strategy switched to {} with weights {}", name, weights);
    }

    private SchedulingStrategy loadOrDefault() {
        try {
            return configRepository.findAll().stream().findFirst()
                .map(config -> {
                    log.info("Loaded scheduling strategy from DB: {}", config.getStrategyName());
                    return SchedulingStrategyFactory.create(config.getStrategyName(), config.getWeights());
                })
                .orElseGet(() -> {
                    log.info("No scheduling config found, defaulting to ROUND_ROBIN");
                    return SchedulingStrategyFactory.create("ROUND_ROBIN", Map.of());
                });
        } catch (Exception e) {
            log.warn("Failed to load scheduling config from DB, defaulting to ROUND_ROBIN: {}", e.getMessage());
            return SchedulingStrategyFactory.create("ROUND_ROBIN", Map.of());
        }
    }
}

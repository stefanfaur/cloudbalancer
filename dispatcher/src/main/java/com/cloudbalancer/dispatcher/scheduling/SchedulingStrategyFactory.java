package com.cloudbalancer.dispatcher.scheduling;

import java.util.Map;

public final class SchedulingStrategyFactory {

    private SchedulingStrategyFactory() {}

    public static SchedulingStrategy create(String name, Map<String, Integer> weights) {
        return switch (name.toUpperCase()) {
            case "ROUND_ROBIN" -> new RoundRobinStrategy();
            case "WEIGHTED_ROUND_ROBIN" -> new WeightedRoundRobinStrategy();
            case "LEAST_CONNECTIONS" -> new LeastConnectionsStrategy();
            case "RESOURCE_FIT" -> new ResourceFitStrategy();
            case "CUSTOM" -> new CustomStrategy(weights != null ? weights : Map.of());
            default -> throw new IllegalArgumentException("Unknown strategy: " + name);
        };
    }
}

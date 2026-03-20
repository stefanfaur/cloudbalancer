package com.cloudbalancer.dispatcher.scheduling;

import java.util.Map;

public class ResourceFitStrategy extends WeightedScoringStrategy {

    public ResourceFitStrategy() {
        super("RESOURCE_FIT", Map.of("resourceAvailability", 80, "queueDepth", 20));
    }
}

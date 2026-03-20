package com.cloudbalancer.dispatcher.scheduling;

import java.util.Map;

public class LeastConnectionsStrategy extends WeightedScoringStrategy {

    public LeastConnectionsStrategy() {
        super("LEAST_CONNECTIONS", Map.of("queueDepth", 100));
    }
}

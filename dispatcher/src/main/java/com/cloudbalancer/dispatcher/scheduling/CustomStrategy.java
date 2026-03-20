package com.cloudbalancer.dispatcher.scheduling;

import java.util.Map;

public class CustomStrategy extends WeightedScoringStrategy {

    public CustomStrategy(Map<String, Integer> weights) {
        super("CUSTOM", weights);
    }
}

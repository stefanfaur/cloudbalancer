package com.cloudbalancer.dispatcher.api.dto;

import java.util.Map;

public record StrategyRequest(String strategy, Map<String, Integer> weights) {}

package com.cloudbalancer.dispatcher.api.dto;

public record WorkerSummary(String id, String healthState, String agentId, int activeTaskCount, String registeredAt) {}

package com.cloudbalancer.dispatcher.api.dto;

public record AgentWorkerResponse(String workerId, String healthState, int activeTaskCount, String registeredAt) {}

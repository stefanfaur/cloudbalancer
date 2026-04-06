package com.cloudbalancer.dispatcher.api.dto;

public record AgentRegistrationRequest(String agentId, String token, int cpuCores, int memoryMb) {}

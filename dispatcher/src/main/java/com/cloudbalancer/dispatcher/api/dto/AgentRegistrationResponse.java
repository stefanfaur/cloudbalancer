package com.cloudbalancer.dispatcher.api.dto;

public record AgentRegistrationResponse(String kafkaBootstrap, String kafkaUsername, String kafkaPassword) {}

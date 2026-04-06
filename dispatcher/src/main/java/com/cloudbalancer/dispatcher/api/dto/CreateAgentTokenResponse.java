package com.cloudbalancer.dispatcher.api.dto;

import java.util.UUID;

public record CreateAgentTokenResponse(UUID id, String token, String label) {}

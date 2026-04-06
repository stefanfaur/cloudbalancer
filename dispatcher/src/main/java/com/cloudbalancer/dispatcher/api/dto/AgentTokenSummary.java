package com.cloudbalancer.dispatcher.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AgentTokenSummary(UUID id, String label, String createdBy,
                                 Instant createdAt, Instant lastUsedAt, boolean revoked) {}
